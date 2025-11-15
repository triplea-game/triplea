package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;
import games.strategy.engine.framework.map.listing.MapListingFetcher;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.triplea.EngineImageLoader;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Serial;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nls;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.java.Interruptibles;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/** Window that allows for map downloads and removal. */
@Slf4j
public class DownloadMapsWindow extends JFrame {

  public static final @Nls String TEXT_ABBREVIATION_NOT_APPLICABLE = "N/A";

  private enum MapAction {
    INSTALL,
    UPDATE,
    REMOVE
  }

  @Serial private static final long serialVersionUID = -1542210716764178580L;
  private static final int WINDOW_WIDTH = 1200;
  private static final int WINDOW_HEIGHT = 700;
  private static final int DIVIDER_POSITION = WINDOW_HEIGHT - 150;
  private static final String MULTIPLE_SELECT_MSG =
      "You can select multiple maps by holding control or shift while clicking map names.";
  private static final SingletonManager SINGLETON_MANAGER = new SingletonManager();

  private final MapDownloadProgressPanel progressPanel;

  private final transient DownloadMapsWindowModel downloadMapsWindowModel;

  private DownloadMapsWindow(
      final Collection<String> pendingDownloadMapNames, final List<MapDownloadItem> allDownloads) {
    super("Download Maps");
    downloadMapsWindowModel = new DownloadMapsWindowModel();

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    setLocationRelativeTo(null);
    setMinimumSize(new Dimension(200, 200));

    setIconImage(EngineImageLoader.loadFrameIcon());
    progressPanel = new MapDownloadProgressPanel();

    final Set<MapDownloadItem> pendingDownloads = new HashSet<>();
    final Collection<String> unknownMapNames = new ArrayList<>();
    for (final String mapName : pendingDownloadMapNames) {
      findMap(mapName, allDownloads)
          .ifPresentOrElse(pendingDownloads::add, () -> unknownMapNames.add(mapName));
    }
    if (!pendingDownloads.isEmpty()) {
      progressPanel.download(pendingDownloads);
    }

    pendingDownloads.addAll(
        DownloadCoordinator.instance.getDownloads().stream()
            .filter(download -> download.getDownloadState() != DownloadState.CANCELLED)
            .map(DownloadFile::getDownload)
            .toList());

    if (!unknownMapNames.isEmpty()) {
      SwingComponents.newMessageDialog(formatIgnoredPendingMapsMessage(unknownMapNames));
    }

    SwingComponents.addWindowClosingListener(this, progressPanel::cancel);

    final Component outerTabs = newAvailableInstalledTabbedPanel(allDownloads, pendingDownloads);

    final JSplitPane splitPane =
        new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, outerTabs, SwingComponents.newJScrollPane(progressPanel));
    splitPane.setDividerLocation(DIVIDER_POSITION);
    add(splitPane);
  }

  /**
   * Shows the Download Maps window.
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   */
  public static void showDownloadMapsWindow() {
    checkState(SwingUtilities.isEventDispatchThread());

    showDownloadMapsWindowAndDownload(List.of());
  }

  /**
   * Shows the Download Maps window and immediately begins downloading the specified map in the
   * background.
   *
   * <p>The user will be notified if the specified map is unknown.
   *
   * @param mapName The name of the map to download; must not be {@code null}.
   * @throws IllegalStateException If this method is not called from the EDT.
   */
  public static void showDownloadMapsWindowAndDownload(final String mapName) {
    checkState(SwingUtilities.isEventDispatchThread());
    checkNotNull(mapName);

    showDownloadMapsWindowAndDownload(List.of(mapName));
  }

  /**
   * Shows the Download Maps window and immediately begins downloading the specified maps in the
   * background.
   *
   * <p>The user will be notified if any of the specified maps are unknown.
   *
   * @param mapNamesToDownload The collection containing the names of the maps to download; must not
   *     be {@code null}.
   * @throws IllegalStateException If this method is not called from the EDT.
   */
  public static void showDownloadMapsWindowAndDownload(
      final Collection<String> mapNamesToDownload) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> showDownloadMapsWindowAndDownload(mapNamesToDownload));
      return;
    }
    checkNotNull(mapNamesToDownload);
    SINGLETON_MANAGER.showAndDownload(mapNamesToDownload);
  }

  /**
   * Returns an HTML-formatted label text based on the current map selection.
   *
   * <p>Return values based on the selection:
   *
   * <ul>
   *   <li><b>One map:</b> the map name
   *   <li><b>Multiple maps:</b> a comma separated list of the names of the selected maps
   *   <li><b>No map:</b> a string indicating there is no selection
   * </ul>
   *
   * @param selectedMapItems List of selected maps
   * @return a descriptive HTML string depending on the selection
   */
  private String newMapUrlAndSizeLabelText(final List<MapDownloadItem> selectedMapItems) {
    if (selectedMapItems.isEmpty()) {
      return "<html>None selected</html>";
    }

    String mapsString =
        String.join(", ", selectedMapItems.stream().map(MapDownloadItem::getMapName).toList());

    final StringBuilder sb = new StringBuilder();
    sb.append("<html>")
        .append(MessageFormat.format("Selected: {0}", mapsString))
        .append("&nbsp;&nbsp;");

    getTextForSizeAndPath(selectedMapItems, downloadMapsWindowModel).ifPresent(sb::append);
    sb.append("<br>");
    sb.append("</html>");

    return sb.toString();
  }

  private static Optional<String> getTextForSizeAndPath(
      final List<MapDownloadItem> selectedMapItems, DownloadMapsWindowModel mapsWindowModel) {
    if (selectedMapItems.size() == 1) {
      final MapDownloadItem selectedMap = selectedMapItems.get(0);
      if (!mapsWindowModel.isInstalled(selectedMap)) {
        if (selectedMap.getDownloadSizeInBytes() != -1L) {
          return Optional.of(
              "(" + FileUtils.byteCountToDisplaySize(selectedMap.getDownloadSizeInBytes()) + ")");
        } else {
          return mapsWindowModel.getByteSizeTextForMap(selectedMap);
        }
      } else {
        return mapsWindowModel.getByteSizeTextForMap(selectedMap);
      }
    } else if (selectedMapItems.size() > 1) {
      return getTextForSizeAndPathFromMaps(selectedMapItems, mapsWindowModel);
    }
    return Optional.empty();
  }

  private static Optional<String> getTextForSizeAndPathFromMaps(
      List<MapDownloadItem> selectedMapItems, final DownloadMapsWindowModel mapsWindowModel) {
    final long totalSizeSelected =
        selectedMapItems.stream()
            .mapToLong(
                mapItem -> {
                  long byteSize = mapItem.getDownloadSizeInBytes();
                  if (byteSize != -1L) {
                    return byteSize;
                  } else {
                    return mapsWindowModel
                        .getInstallLocation(mapItem)
                        .map(
                            mapPath -> {
                              try {
                                return org.triplea.io.FileUtils.getByteSizeFromPath(mapPath);
                              } catch (IOException e) {
                                return 0L;
                              }
                            })
                        .orElse(0L);
                  }
                })
            .sum();
    return Optional.of("(total: " + FileUtils.byteCountToDisplaySize(totalSizeSelected) + ")");
  }

  private static String formatIgnoredPendingMapsMessage(final Collection<String> unknownMapNames) {
    final StringBuilder sb = new StringBuilder();
    sb.append("<html>");
    sb.append("Some maps were not downloaded.<br>");

    if (!unknownMapNames.isEmpty()) {
      sb.append("<br>");
      sb.append("Could not find the following map(s):<br>");
      sb.append("<ul>");
      for (final String mapName : unknownMapNames) {
        sb.append("<li>").append(mapName).append("</li>");
      }
      sb.append("</ul>");
    }

    sb.append("</html>");
    return sb.toString();
  }

  private static Optional<MapDownloadItem> findMap(
      final String mapName, final List<MapDownloadItem> games) {

    final String normalizedName = normalizeName(mapName);
    for (final MapDownloadItem download : games) {
      if (download.getMapName().equalsIgnoreCase(mapName)
          || normalizedName.equals(normalizeName(download.getMapName()))) {
        return Optional.of(download);
      }
    }
    return Optional.empty();
  }

  private static String normalizeName(final String mapName) {
    return mapName.replace(' ', '_').toLowerCase(Locale.ROOT);
  }

  private JTabbedPane newAvailableInstalledTabbedPanel(
      final List<MapDownloadItem> downloads, final Set<MapDownloadItem> pendingDownloads) {
    final DownloadMapsWindowMapsListing mapList = new DownloadMapsWindowMapsListing(downloads);

    final JTabbedPane tabbedPane = new JTabbedPane();

    final List<MapDownloadItem> outOfDateDownloads =
        mapList.getOutOfDateExcluding(pendingDownloads);
    // For the UX, always show an available maps tab, even if it is empty
    final JPanel available =
        newMapSelectionPanel(
            mapList.getAvailableExcluding(pendingDownloads), MapAction.INSTALL, true);
    tabbedPane.addTab("New Maps", available);

    if (!outOfDateDownloads.isEmpty()) {
      final JPanel outOfDate = newMapSelectionPanel(outOfDateDownloads, MapAction.UPDATE, false);
      tabbedPane.addTab("Updates Available", outOfDate);
    }

    if (!mapList.getInstalled().isEmpty()) {
      final JPanel installed =
          newMapSelectionPanel(
              mapList.getInstalled().keySet().stream()
                  .sorted(Comparator.comparing(m -> m.getMapName().toUpperCase(Locale.ENGLISH)))
                  .toList(),
              MapAction.REMOVE,
              false);
      tabbedPane.addTab("Installed", installed);
    }
    return tabbedPane;
  }

  private JPanel newMapSelectionPanel(
      final List<MapDownloadItem> unsortedMaps,
      final MapAction action,
      final boolean requestFocus) {
    final JPanel main = new JPanelBuilder().border(30).borderLayout().build();
    final JEditorPane descriptionPane = SwingComponents.newHtmlJEditorPane();
    main.add(SwingComponents.newJScrollPane(descriptionPane), BorderLayout.CENTER);

    final JLabel mapSizeLabel = new JLabel(" ");

    if (!unsortedMaps.isEmpty()) {
      final MapDownloadSwingTable mapDownloadSwingTable = new MapDownloadSwingTable(unsortedMaps);
      final JTable gamesList = mapDownloadSwingTable.getSwingComponent();
      mapDownloadSwingTable.addMapSelectionListener(
          mapSelections ->
              newDescriptionPanelUpdatingSelectionListener(
                  mapSelections, descriptionPane, unsortedMaps, mapSizeLabel));

      descriptionPane.setText(downloadMapsWindowModel.toHtmlString(unsortedMaps.get(0)));
      descriptionPane.scrollRectToVisible(new Rectangle(0, 0, 0, 0));

      // Create label and search field
      final JLabel searchLabel = new JLabel("Search:");
      final JTextField searchField = new JTextField(15);
      searchField.setToolTipText("Search maps...");
      if (requestFocus) {
        SwingUtilities.invokeLater(searchField::requestFocus);
      }

      // Panel for label + field
      JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
      searchPanel.add(searchLabel);
      searchPanel.add(searchField);

      // Row sorter for filtering
      TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(gamesList.getModel());
      gamesList.setRowSorter(rowSorter);
      searchField
          .getDocument()
          .addDocumentListener(
              new DocumentListener() {
                private void updateFilter() {
                  String text = searchField.getText();
                  if (text.trim().isEmpty()) {
                    rowSorter.setRowFilter(null);
                  } else {
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
                  }
                }

                public void insertUpdate(DocumentEvent e) {
                  updateFilter();
                }

                public void removeUpdate(DocumentEvent e) {
                  updateFilter();
                }

                public void changedUpdate(DocumentEvent e) {
                  updateFilter();
                }
              });

      // Layout: put search box above games list
      JPanel leftPanel = new JPanel(new BorderLayout());
      leftPanel.add(searchPanel, BorderLayout.NORTH);
      leftPanel.add(SwingComponents.newJScrollPane(gamesList), BorderLayout.CENTER);
      main.add(leftPanel, BorderLayout.WEST);

      // sort initially and select first entry by default
      if (gamesList.getRowCount() > 0) {
        rowSorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        rowSorter.sort();
        gamesList.setRowSelectionInterval(0, 0);
      }

      final JPanel southPanel =
          new JPanelBuilder()
              .gridLayout(2, 1)
              .add(mapSizeLabel)
              .add(
                  newButtonsPanel(
                      action,
                      mapDownloadSwingTable::getSelectedMapNames,
                      unsortedMaps,
                      mapDownloadSwingTable::removeMapRow))
              .build();
      main.add(southPanel, BorderLayout.SOUTH);
    }

    return main;
  }

  private void newDescriptionPanelUpdatingSelectionListener(
      final List<String> mapNames,
      final JEditorPane descriptionPanel,
      final List<MapDownloadItem> maps,
      final JLabel mapSizeLabelToUpdate) {

    List<MapDownloadItem> selectedMapItems = getSelectedMapDownloadItems(mapNames, maps);

    final String newMapSizeLabelText = newMapUrlAndSizeLabelText(selectedMapItems);
    mapSizeLabelToUpdate.setText(newMapSizeLabelText);

    final String newDescriptionPanelText = newDescriptionPanelText(selectedMapItems);
    descriptionPanel.setText(newDescriptionPanelText);
    descriptionPanel.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
  }

  /**
   * Returns an HTML-formatted label for the description panel based on the current map selection.
   *
   * <p>Return values:
   *
   * <ul>
   *   <li><b>Single map selected:</b> the result of {@link
   *       DownloadMapsWindowModel#toHtmlString(MapDownloadItem)}
   *   <li><b>Multiple maps selected:</b> a generic message indicating multiple maps are selected
   *   <li><b>No selection:</b> an empty string
   * </ul>
   *
   * @param selectedMapItems List of selected maps
   * @return a descriptive HTML string depending on the selection
   */
  private String newDescriptionPanelText(List<MapDownloadItem> selectedMapItems) {
    final int countSelectedMapItems = selectedMapItems.size();
    final String descriptionPanelText;
    if (countSelectedMapItems == 1) {
      descriptionPanelText = downloadMapsWindowModel.toHtmlString(selectedMapItems.get(0));
    } else if (countSelectedMapItems > 1) {
      descriptionPanelText = "(multiple maps selected)";
    } else {
      descriptionPanelText = "";
    }
    return descriptionPanelText;
  }

  /**
   * Returns a sublist of {@code maps} which entries match one entry in {@code mapNames} by {@link
   * MapDownloadItem#getMapName}.
   *
   * @param mapNames List of Strings for map names
   * @param maps List of MapDownloadItem that are available
   * @return Matching MapDownloadItem sublist
   */
  private static @Nonnull List<MapDownloadItem> getSelectedMapDownloadItems(
      List<String> mapNames, List<MapDownloadItem> maps) {
    List<MapDownloadItem> selectedMapItems = new ArrayList<>();
    for (final MapDownloadItem map : maps) {
      if (mapNames.contains(map.getMapName())) {
        selectedMapItems.add(map);
        if (selectedMapItems.size() == mapNames.size()) {
          break;
        }
      }
    }
    return selectedMapItems;
  }

  private static final class SingletonManager {
    private enum State {
      UNINITIALIZED,
      INITIALIZING,
      INITIALIZED
    }

    private State state;
    private DownloadMapsWindow window;

    SingletonManager() {
      uninitialize();
    }

    private void uninitialize() {
      assert SwingUtilities.isEventDispatchThread();

      state = State.UNINITIALIZED;
      window = null;
    }

    private static void logMapDownloadRequestIgnored(final Collection<String> mapNames) {
      if (!mapNames.isEmpty()) {
        log.info(
            "ignoring request to download maps because window initialization has already started");
      }
    }

    void showAndDownload(final Collection<String> mapNamesToDownload) {
      assert SwingUtilities.isEventDispatchThread();

      switch (state) {
        case UNINITIALIZED:
          initialize(mapNamesToDownload);
          break;

        case INITIALIZING:
          logMapDownloadRequestIgnored(mapNamesToDownload);
          // do nothing; window will be shown when initialization is complete
          break;

        case INITIALIZED:
          logMapDownloadRequestIgnored(mapNamesToDownload);
          show();
          break;

        default:
          throw new AssertionError("unknown state");
      }
    }

    private void initialize(final Collection<String> mapNamesToDownload) {
      assert SwingUtilities.isEventDispatchThread();
      assert state == State.UNINITIALIZED;

      Interruptibles.awaitResult(SingletonManager::getMapDownloadListInBackground)
          .result
          .ifPresent(
              downloads -> {
                if (downloads.isEmpty()) {
                  return; // no maps to show, so we can leave
                }
                state = State.INITIALIZING;
                createAndShow(mapNamesToDownload, downloads);
              });
    }

    private static List<MapDownloadItem> getMapDownloadListInBackground()
        throws InterruptedException {
      return BackgroundTaskRunner.runInBackgroundAndReturn(
          "Downloading list of available maps...", MapListingFetcher::getMapDownloadList);
    }

    private void createAndShow(
        final Collection<String> mapNamesToDownload,
        final List<MapDownloadItem> availableDownloads) {
      assert SwingUtilities.isEventDispatchThread();
      assert state == State.INITIALIZING;
      assert window == null;

      window = new DownloadMapsWindow(mapNamesToDownload, availableDownloads);
      SwingComponents.addWindowClosedListener(window, this::uninitialize);
      LookAndFeelSwingFrameListener.register(window);
      state = State.INITIALIZED;

      show();
    }

    private void show() {
      assert SwingUtilities.isEventDispatchThread();
      assert state == State.INITIALIZED;
      assert window != null;

      window.setVisible(true);
      window.requestFocus();
      window.toFront();
    }
  }

  private JPanel newButtonsPanel(
      final MapAction action,
      final Supplier<List<String>> mapSelection,
      final List<MapDownloadItem> maps,
      final Consumer<String> tableRemoveAction) {

    return new JPanelBuilder()
        .border(20)
        .gridLayout(1, 5)
        .add(buildMapActionButton(action, mapSelection, maps, tableRemoveAction))
        .add(Box.createGlue())
        .add(
            new JButtonBuilder()
                .title("Close")
                .toolTip(
                    "Click this button to close the map download window and cancel any in-progress downloads.")
                .actionListener(
                    () -> {
                      setVisible(false);
                      dispose();
                    })
                .build())
        .build();
  }

  private JButton buildMapActionButton(
      final MapAction action,
      final Supplier<List<String>> mapSelection,
      final List<MapDownloadItem> maps,
      final Consumer<String> tableRemoveAction) {
    final JButton actionButton;

    if (action == MapAction.REMOVE) {
      actionButton =
          new JButtonBuilder()
              .title("Remove")
              .toolTip(
                  MessageFormat.format(
                      "Click this button to remove the maps selected above from your computer. {0}",
                      MULTIPLE_SELECT_MSG))
              .actionListener(removeAction(mapSelection, maps, tableRemoveAction))
              .build();
    } else {
      actionButton =
          new JButtonBuilder()
              .title((action == MapAction.INSTALL) ? "Install" : "Update")
              .toolTip(
                  MessageFormat.format(
                      "Click this button to download and install the maps selected above. {0}",
                      MULTIPLE_SELECT_MSG))
              .actionListener(installAction(mapSelection, maps, tableRemoveAction))
              .build();
    }
    return actionButton;
  }

  private Runnable removeAction(
      final Supplier<List<String>> mapSelection,
      final List<MapDownloadItem> maps,
      final Consumer<String> tableRemoveAction) {
    return () -> {
      final List<String> selectedValues = mapSelection.get();
      final List<MapDownloadItem> selectedMaps =
          maps.stream().filter(map -> selectedValues.contains(map.getMapName())).toList();
      if (!selectedMaps.isEmpty()) {
        FileSystemAccessStrategy.remove(
            downloadMapsWindowModel::delete, selectedMaps, tableRemoveAction);
      }
    };
  }

  private Runnable installAction(
      final Supplier<List<String>> mapSelection,
      final List<MapDownloadItem> maps,
      final Consumer<String> tableRemoveAction) {
    return () -> {
      final List<String> selectedValues = mapSelection.get();
      final List<MapDownloadItem> downloadList =
          maps.stream().filter(map -> selectedValues.contains(map.getMapName())).toList();

      if (!downloadList.isEmpty()) {
        progressPanel.download(downloadList);
      }

      downloadList.stream().map(MapDownloadItem::getMapName).forEach(tableRemoveAction);
    };
  }
}
