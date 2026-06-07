package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NonNls;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.java.Interruptibles;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/** Window that allows for map downloads and removal. */
@Slf4j
public class DownloadMapsWindow extends JFrame {

  private final JTabbedPane tabbedPane;
  private MapTab availableMapTab;
  private MapTab outOfDateMapTab = null;
  private MapTab installedMapTab;

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
      final Collection<String> pendingDownloadMapNames,
      DownloadMapsWindowModel downloadMapsWindowModel) {
    super("Download Maps");
    this.downloadMapsWindowModel = downloadMapsWindowModel;
    getMapStore().addMapStatusListener(this::handleMapStatusChange);

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    setLocationRelativeTo(null);
    setMinimumSize(new Dimension(200, 200));

    setIconImage(EngineImageLoader.loadFrameIcon());
    progressPanel = new MapDownloadProgressPanel();

    final Set<MapDownloadItem> pendingDownloads = new HashSet<>();
    final Collection<String> unknownMapNames = new ArrayList<>();
    for (final String mapName : pendingDownloadMapNames) {
      findMapByName(mapName)
          .map(ManagedMap::getMapDownloadItem)
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
      SwingComponents.newMessageDialog(this, formatIgnoredPendingMapsMessage(unknownMapNames));
    }

    SwingComponents.addWindowClosingListener(this, progressPanel::cancel);

    tabbedPane = newAvailableInstalledTabbedPanel();
    updateTabTitles();

    final JSplitPane splitPane =
        new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, tabbedPane, SwingComponents.newJScrollPane(progressPanel));
    splitPane.setDividerLocation(DIVIDER_POSITION);
    add(splitPane);
  }

  private void handleMapStatusChange(ManagedMapStatus oldStatus, ManagedMapStatus newStatus) {
    switch (newStatus) {
      case ManagedMapStatus.AVAILABLE -> {
        updateTabTitleNewMaps();
        availableMapTab.setDirty();
      }
      case ManagedMapStatus.INSTALLED, ManagedMapStatus.REMOVING -> {
        updateTabTitleInstalled();
        if (newStatus == ManagedMapStatus.INSTALLED) {
          installedMapTab.setDirty();
        }
      }
      case ManagedMapStatus.DOWNLOADING -> {
        if (oldStatus == ManagedMapStatus.UPDATE_AVAILABLE) {
          updateTabTitleUpdatesAvailable();
        } else {
          updateTabTitleNewMaps();
        }
      }
    }
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
  private String newMapUrlAndSizeLabelText(final List<ManagedMap> selectedMapItems) {
    if (selectedMapItems.isEmpty()) {
      return "<html>None selected</html>";
    }
    final @NonNls String doubleSpace = "&nbsp;&nbsp;";

    String mapsString =
        String.join(", ", selectedMapItems.stream().map(ManagedMap::getMapName).toList());

    final StringBuilder sb = new StringBuilder();
    sb.append("<html>").append(String.format("Selected: %s", mapsString)).append(doubleSpace);

    if (selectedMapItems.size() == 1) {
      final ManagedMap map = selectedMapItems.getFirst();
      MapDownloadItem mapDownloadItem = map.getMapDownloadItem();
      if (!downloadMapsWindowModel.isInstalled(map)) {
        if (mapDownloadItem.getDownloadSizeInBytes() != -1L) {
          sb.append(doubleSpace)
              .append(" (")
              .append(FileUtils.byteCountToDisplaySize(mapDownloadItem.getDownloadSizeInBytes()))
              .append(")");
        }
      } else {
        downloadMapsWindowModel
            .getInstallLocation(map)
            .ifPresent(
                mapPath -> {
                  sb.append(doubleSpace).append(" (");
                  try {
                    sb.append(FileUtils.byteCountToDisplaySize(Files.size(mapPath)));
                  } catch (final IOException e) {
                    log.warn("Failed to read file size", e);
                    sb.append("N/A");
                  }
                  sb.append(")")
                      .append("<br>")
                      .append(String.format("Path: %s", mapPath.toAbsolutePath()));
                });
      }
    }
    sb.append("<br>");
    sb.append("</html>");

    return sb.toString();
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

  private Optional<ManagedMap> findMapByName(final String mapName) {
    Optional<ManagedMap> mapByName = getMapStore().getMapByName(mapName);
    if (mapByName.isPresent()) {
      return mapByName;
    }
    final String normalizedName = InstalledMapsListing.normalizeName(mapName);
    return getMapStore().getMapByName(normalizedName);
  }

  private JTabbedPane newAvailableInstalledTabbedPanel() {

    final JTabbedPane tabbedPane = new JTabbedPane();

    // For the UX, always show an available maps tab, even if it is empty
    availableMapTab = new MapTab(true, MapAction.INSTALL, ManagedMapStatus.AVAILABLE);
    tabbedPane.addTab("", availableMapTab.getContentPanel());

    boolean mapStoreHasUpdateAvailable = getMapStore().hasAnyMap(ManagedMapStatus.UPDATE_AVAILABLE);
    if (mapStoreHasUpdateAvailable) {
      outOfDateMapTab = new MapTab(false, MapAction.UPDATE, ManagedMapStatus.UPDATE_AVAILABLE);
      tabbedPane.addTab("", outOfDateMapTab.getContentPanel());
    }

    installedMapTab =
        new MapTab(
            false, MapAction.REMOVE, ManagedMapStatus.INSTALLED, ManagedMapStatus.UPDATE_AVAILABLE);
    tabbedPane.addTab("", installedMapTab.getContentPanel());

    tabbedPane.addChangeListener(
        e -> {
          MapTab selectedTab = getSelectedMapTab();
          if (selectedTab.isUpToDate()) {
            return;
          }
          selectedTab.refreshFromStore();
        });

    return tabbedPane;
  }

  private MapTab getSelectedMapTab() {
    Component selectedTabComponent = tabbedPane.getSelectedComponent();
    if (selectedTabComponent.equals(availableMapTab.getContentPanel())) {
      return availableMapTab;
    }
    if (installedMapTab != null && selectedTabComponent.equals(installedMapTab.getContentPanel())) {
      return installedMapTab;
    }
    if (outOfDateMapTab != null && selectedTabComponent.equals(outOfDateMapTab.getContentPanel())) {
      return outOfDateMapTab;
    }
    throw new IllegalStateException("No map tab is recognized as being selected.");
  }

  private ManagedMapStore getMapStore() {
    return downloadMapsWindowModel.getMapStore();
  }

  private void updateTabTitles() {
    updateTabTitleNewMaps();
    updateTabTitleUpdatesAvailable();
    updateTabTitleInstalled();
  }

  private void updateTabTitleNewMaps() {
    setTabTitle(
        availableMapTab.getContentPanel(),
        String.format("New Maps (%d)", getMapStore().getCountByStatus(ManagedMapStatus.AVAILABLE)));
  }

  private void updateTabTitleUpdatesAvailable() {
    if (outOfDateMapTab == null) {
      return; // no tab available to update its title
    }
    setTabTitle(
        outOfDateMapTab.getContentPanel(),
        String.format(
            "Updates Available (%d)",
            getMapStore().getCountByStatus(ManagedMapStatus.UPDATE_AVAILABLE)));
  }

  private void updateTabTitleInstalled() {
    setTabTitle(
        installedMapTab.getContentPanel(),
        String.format(
            "Installed (%d)",
            getMapStore().getCountByStatus(ManagedMapStatus.INSTALLED)
                + getMapStore().getCountByStatus(ManagedMapStatus.UPDATE_AVAILABLE)));
  }

  private void setTabTitle(Component tab, String title) {
    int index = tabbedPane.indexOfComponent(tab);
    if (index != -1) {
      tabbedPane.setTitleAt(index, title);
    }
  }

  private void newDescriptionPanelUpdatingSelectionListener(
      final List<ManagedMap> selectedMaps,
      final JEditorPane descriptionPanel,
      final JLabel mapSizeLabelToUpdate) {

    final String newMapSizeLabelText = newMapUrlAndSizeLabelText(selectedMaps);
    mapSizeLabelToUpdate.setText(newMapSizeLabelText);

    final String newDescriptionPanelText = newDescriptionPanelText(selectedMaps);
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
   *       DownloadMapsWindowModel#toHtmlString(ManagedMap)}
   *   <li><b>Multiple maps selected:</b> a generic message indicating multiple maps are selected
   *   <li><b>No selection:</b> an empty string
   * </ul>
   *
   * @param selectedMapItems List of selected maps
   * @return a descriptive HTML string depending on the selection
   */
  private String newDescriptionPanelText(List<ManagedMap> selectedMapItems) {
    final int countSelectedMapItems = selectedMapItems.size();
    final String descriptionPanelText;
    if (countSelectedMapItems == 1) {
      descriptionPanelText = downloadMapsWindowModel.toHtmlString(selectedMapItems.getFirst());
    } else if (countSelectedMapItems > 1) {
      descriptionPanelText = "(multiple maps selected)";
    } else {
      descriptionPanelText = "";
    }
    return descriptionPanelText;
  }

  enum MapAction {
    INSTALL,
    UPDATE,
    REMOVE
  }

  enum MapTabStatus {
    NOT_INITIALIZED,
    DIRTY,
    UP_TO_DATE
  }

  /// Represents a single tab in the map download window.
  ///
  /// A `MapTab` is responsible for displaying maps that match one or more
  /// {@link ManagedMapStatus} values and exposing the action that can be performed
  /// on those maps (for example install, update, or remove).
  ///
  /// <p>The tab maintains its UI state independently of the underlying
  /// {@link ManagedMapStore}. When map status changes occur for currently hidden tabs, it can be
  /// marked as dirty (i.e. requiring refresh) and will rebuild its table contents from the current
  /// store state when becoming visible again.
  class MapTab {
    private final ManagedMapStatus[] mapStatuses;
    private final MapAction mapAction;
    private MapDownloadSwingTable mapDownloadTable = null;
    @Getter private final JPanel contentPanel;
    private MapTabStatus mapTabStatus;

    MapTab(boolean buildImmediately, MapAction mapAction, ManagedMapStatus... mapStatuses) {
      this.mapStatuses = mapStatuses;
      this.mapAction = mapAction;
      this.contentPanel = new JPanelBuilder().border(30).borderLayout().build();
      if (buildImmediately) {
        initializeTabContentPanel();
      } else {
        mapTabStatus = MapTabStatus.NOT_INITIALIZED;
      }
    }

    void initializeTabContentPanel() {
      final JEditorPane descriptionPane = SwingComponents.newHtmlJEditorPane();
      contentPanel.add(SwingComponents.newJScrollPane(descriptionPane), BorderLayout.CENTER);

      final JLabel mapSizeLabel = new JLabel(" ");

      if (getMapStore().hasAnyMap(mapStatuses)) {

        final List<ManagedMap> unsortedMaps = getMapsListFromStore();
        mapDownloadTable = new MapDownloadSwingTable(unsortedMaps);
        final JTable gamesList = mapDownloadTable.getSwingComponent();
        mapDownloadTable.addMapSelectionListener(
            mapSelections ->
                newDescriptionPanelUpdatingSelectionListener(
                    getMapStore().getMapsByName(() -> mapSelections),
                    descriptionPane,
                    mapSizeLabel));

        descriptionPane.setText(downloadMapsWindowModel.toHtmlString(unsortedMaps.getFirst()));
        descriptionPane.scrollRectToVisible(new Rectangle(0, 0, 0, 0));

        // Create label and search field
        final JLabel searchLabel = new JLabel("Search:");
        final JTextField searchField = new JTextField(15);
        searchField.setToolTipText("Search maps...");
        SwingUtilities.invokeLater(searchField::requestFocus);

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
        contentPanel.add(leftPanel, BorderLayout.WEST);

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
                        mapAction,
                        () -> getMapStore().getMapsByName(mapDownloadTable::getSelectedMapNames),
                        mapDownloadTable::removeMapRows))
                .build();
        contentPanel.add(southPanel, BorderLayout.SOUTH);

        mapTabStatus = MapTabStatus.UP_TO_DATE;
      }
    }

    private List<ManagedMap> getMapsListFromStore() {
      return getMapStore().getByStatus(mapStatuses);
    }

    boolean isUpToDate() {
      return this.mapTabStatus == MapTabStatus.UP_TO_DATE;
    }

    void setDirty() {
      if (mapTabStatus == MapTabStatus.NOT_INITIALIZED) {
        return; // leave not-initialized until show first
      }
      this.mapTabStatus = MapTabStatus.DIRTY;
    }

    void refreshFromStore() {
      if (mapTabStatus == MapTabStatus.NOT_INITIALIZED) {
        initializeTabContentPanel();
      } else {
        mapDownloadTable.setMaps(getMapsListFromStore());
        this.mapTabStatus = MapTabStatus.UP_TO_DATE;
      }
    }
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
              downloadMapsWindowModel -> {
                if (downloadMapsWindowModel.getMapStore().isEmpty()) {
                  return; // no maps to show, so we can leave
                }
                state = State.INITIALIZING;

                createAndShow(mapNamesToDownload, downloadMapsWindowModel);
              });
    }

    private static DownloadMapsWindowModel getMapDownloadListInBackground()
        throws InterruptedException {
      return BackgroundTaskRunner.runInBackgroundAndReturn(
          "Downloading list of available maps...",
          () -> new DownloadMapsWindowModel(MapListingFetcher.getMapDownloadList()));
    }

    private void createAndShow(
        final Collection<String> mapNamesToDownload,
        final DownloadMapsWindowModel downloadMapsWindowModel) {
      assert SwingUtilities.isEventDispatchThread();
      assert state == State.INITIALIZING;
      assert window == null;

      window = new DownloadMapsWindow(mapNamesToDownload, downloadMapsWindowModel);
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
      final Supplier<List<ManagedMap>> mapSelection,
      final Consumer<List<ManagedMap>> tableRemoveAction) {

    return new JPanelBuilder()
        .border(20)
        .gridLayout(1, 5)
        .add(buildMapActionButton(action, mapSelection, tableRemoveAction))
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
      final Supplier<List<ManagedMap>> mapSelection,
      final Consumer<List<ManagedMap>> tableRemoveAction) {
    final JButton actionButton;

    if (action == MapAction.REMOVE) {
      actionButton =
          new JButtonBuilder()
              .title("Remove")
              .toolTip(
                  String.format(
                      "Click this button to remove the maps selected above from your computer. %s",
                      MULTIPLE_SELECT_MSG))
              .actionListener(removeAction(mapSelection, tableRemoveAction))
              .build();
    } else {
      actionButton =
          new JButtonBuilder()
              .title((action == MapAction.INSTALL) ? "Install" : "Update")
              .toolTip(
                  String.format(
                      "Click this button to download and install the maps selected above. %s",
                      MULTIPLE_SELECT_MSG))
              .actionListener(installAction(mapSelection, tableRemoveAction))
              .build();
    }
    return actionButton;
  }

  private Runnable removeAction(
      final Supplier<List<ManagedMap>> mapSelection,
      final Consumer<List<ManagedMap>> tableRemoveAction) {
    return () -> {
      final List<ManagedMap> selectedMaps = mapSelection.get();
      if (!selectedMaps.isEmpty()) {
        getMapStore().updateStatus(selectedMaps, ManagedMapStatus.REMOVING);
        FileSystemAccessStrategy.remove(
            this, downloadMapsWindowModel::delete, selectedMaps, tableRemoveAction);
      }
    };
  }

  private Runnable installAction(
      final Supplier<List<ManagedMap>> mapSelection,
      final Consumer<List<ManagedMap>> tableRemoveAction) {
    return () -> {
      final List<ManagedMap> selectedMaps = mapSelection.get();
      if (!selectedMaps.isEmpty()) {
        getMapStore().updateStatus(selectedMaps, ManagedMapStatus.DOWNLOADING);
        progressPanel.download(selectedMaps.stream().map(ManagedMap::getMapDownloadItem).toList());
        tableRemoveAction.accept(selectedMaps);
      }
    };
  }
}
