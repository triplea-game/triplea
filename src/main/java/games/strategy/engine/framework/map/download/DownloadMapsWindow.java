package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;
import games.strategy.ui.SwingComponents;
import games.strategy.util.OptionalUtils;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

/** Window that allows for map downloads and removal. */
public class DownloadMapsWindow extends JFrame {
  private enum MapAction {
    INSTALL, UPDATE, REMOVE
  }

  private static final long serialVersionUID = -1542210716764178580L;
  private static final Logger logger = Logger.getLogger(DownloadMapsWindow.class.getName());
  private static final int WINDOW_WIDTH = 1200;
  private static final int WINDOW_HEIGHT = 700;
  private static final int DIVIDER_POSITION = WINDOW_HEIGHT - 150;
  private static final SingletonManager SINGLETON_MANAGER = new SingletonManager();

  private final MapDownloadProgressPanel progressPanel;

  /**
   * Shows the Download Maps window.
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   */
  public static void showDownloadMapsWindow() {
    checkState(SwingUtilities.isEventDispatchThread());

    showDownloadMapsWindowAndDownload(Collections.emptyList());
  }

  /**
   * Shows the Download Maps window and immediately begins downloading the specified map in the background.
   *
   * <p>
   * The user will be notified if the specified map is unknown.
   * </p>
   *
   * @param mapName The name of the map to download; must not be {@code null}.
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   */
  public static void showDownloadMapsWindowAndDownload(final String mapName) {
    checkState(SwingUtilities.isEventDispatchThread());
    checkNotNull(mapName);

    showDownloadMapsWindowAndDownload(Collections.singletonList(mapName));
  }

  /**
   * Shows the Download Maps window and immediately begins downloading the specified maps in the background.
   *
   * <p>
   * The user will be notified if any of the specified maps are unknown.
   * </p>
   *
   * @param mapNames The collection containing the names of the maps to download; must not be {@code null}.
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   */
  public static void showDownloadMapsWindowAndDownload(final Collection<String> mapNames) {
    checkState(SwingUtilities.isEventDispatchThread());
    checkNotNull(mapNames);

    SINGLETON_MANAGER.showAndDownload(mapNames);
  }

  private static final class SingletonManager {
    private enum State {
      UNINITIALIZED, INITIALIZING, INITIALIZED
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

    void showAndDownload(final Collection<String> mapNames) {
      assert SwingUtilities.isEventDispatchThread();

      switch (state) {
        case UNINITIALIZED:
          initialize(mapNames);
          break;

        case INITIALIZING:
          logMapDownloadRequestIgnored(mapNames);
          // do nothing; window will be shown when initialization is complete
          break;

        case INITIALIZED:
          logMapDownloadRequestIgnored(mapNames);
          show();
          break;

        default:
          throw new AssertionError("unknown state");
      }
    }

    private void initialize(final Collection<String> mapNames) {
      assert SwingUtilities.isEventDispatchThread();
      assert state == State.UNINITIALIZED;

      state = State.INITIALIZING;
      try {
        final List<DownloadFileDescription> downloads = GameRunner.newBackgroundTaskRunner().runInBackgroundAndReturn(
            "Downloading list of available maps...",
            ClientContext::getMapDownloadList);
        createAndShow(mapNames, downloads);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    private void createAndShow(final Collection<String> mapNames, final List<DownloadFileDescription> downloads) {
      assert SwingUtilities.isEventDispatchThread();
      assert state == State.INITIALIZING;
      assert window == null;

      window = new DownloadMapsWindow(mapNames, downloads);
      SwingComponents.addWindowClosedListener(window, this::uninitialize);
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

  private static void logMapDownloadRequestIgnored(final Collection<String> mapNames) {
    if (!mapNames.isEmpty()) {
      logger.info("ignoring request to download maps because window initialization has already started");
    }
  }

  private DownloadMapsWindow(
      final Collection<String> pendingDownloadMapNames,
      final List<DownloadFileDescription> allDownloads) {
    super("Download Maps");

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    setLocationRelativeTo(null);
    setMinimumSize(new Dimension(200, 200));

    setIconImage(GameRunner.getGameIcon(this));
    progressPanel = new MapDownloadProgressPanel();

    final Set<DownloadFileDescription> pendingDownloads = new HashSet<>();
    final Collection<String> unknownMapNames = new ArrayList<>();
    for (final String mapName : pendingDownloadMapNames) {
      OptionalUtils.ifPresentOrElse(findMap(mapName, allDownloads),
          pendingDownloads::add,
          () -> unknownMapNames.add(mapName));
    }
    final Collection<String> installedMapNames = removeInstalledDownloads(pendingDownloads);

    if (!pendingDownloads.isEmpty()) {
      progressPanel.download(pendingDownloads);
    }

    pendingDownloads.addAll(ClientContext.downloadCoordinator().getDownloads().stream()
        .filter(download -> !download.getDownloadState().equals(DownloadState.CANCELLED))
        .map(DownloadFile::getDownload)
        .collect(Collectors.toList()));

    if (!unknownMapNames.isEmpty() || !installedMapNames.isEmpty()) {
      SwingComponents.newMessageDialog(formatIgnoredPendingMapsMessage(unknownMapNames, installedMapNames));
    }

    final Optional<String> selectedMapName = pendingDownloadMapNames.stream().findFirst();

    SwingComponents.addWindowClosingListener(this, progressPanel::cancel);

    final JTabbedPane outerTabs = new JTabbedPane();

    final List<DownloadFileDescription> maps = filterMaps(allDownloads, DownloadFileDescription::isMap);
    outerTabs.add("Maps", createdTabbedPanelForMaps(maps, pendingDownloads));

    final List<DownloadFileDescription> skins = filterMaps(allDownloads, DownloadFileDescription::isMapSkin);
    outerTabs.add("Skins", createAvailableInstalledTabbedPanel(selectedMapName, skins, pendingDownloads));

    final List<DownloadFileDescription> tools = filterMaps(allDownloads, DownloadFileDescription::isMapTool);
    outerTabs.add("Tools", createAvailableInstalledTabbedPanel(selectedMapName, tools, pendingDownloads));

    final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outerTabs,
        SwingComponents.newJScrollPane(progressPanel));
    splitPane.setDividerLocation(DIVIDER_POSITION);
    add(splitPane);
  }

  private static Collection<String> removeInstalledDownloads(
      final Collection<DownloadFileDescription> downloads) {
    final MapDownloadList mapList = new MapDownloadList(downloads);
    final Collection<DownloadFileDescription> installedDownloads = downloads.stream()
        .filter(mapList::isInstalled)
        .collect(Collectors.toList());
    downloads.removeAll(installedDownloads);
    return installedDownloads.stream()
        .map(DownloadFileDescription::getMapName)
        .collect(Collectors.toList());
  }

  private static String formatIgnoredPendingMapsMessage(
      final Collection<String> unknownMapNames,
      final Collection<String> installedMapNames) {
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

    if (!installedMapNames.isEmpty()) {
      sb.append("<br>");
      sb.append("The following map(s) are already installed:<br>");
      sb.append("<ul>");
      for (final String mapName : installedMapNames) {
        sb.append("<li>").append(mapName).append("</li>");
      }
      sb.append("</ul>");
    }

    sb.append("</html>");
    return sb.toString();
  }

  private Component createdTabbedPanelForMaps(
      final List<DownloadFileDescription> downloads,
      final Set<DownloadFileDescription> pendingDownloads) {
    final JTabbedPane mapTabs = SwingComponents.newJTabbedPane();
    for (final DownloadFileDescription.MapCategory mapCategory : DownloadFileDescription.MapCategory.values()) {
      final List<DownloadFileDescription> categorizedDownloads = downloads.stream()
          .filter(download -> download.getMapCategory() == mapCategory)
          .collect(Collectors.toList());
      if (!categorizedDownloads.isEmpty()) {
        final JTabbedPane subTab = createAvailableInstalledTabbedPanel(Optional.of(mapCategory.toString()),
            categorizedDownloads, pendingDownloads);
        mapTabs.add(mapCategory.toString(), subTab);
      }
    }
    return mapTabs;
  }

  private static Optional<DownloadFileDescription> findMap(final String mapName,
      final List<DownloadFileDescription> games) {

    final String normalizedName = normalizeName(mapName);
    for (final DownloadFileDescription download : games) {
      if (download.getMapName().equalsIgnoreCase(mapName)
          || normalizedName.equals(normalizeName(download.getMapName()))) {
        return Optional.of(download);
      }
    }
    return Optional.empty();
  }

  private static String normalizeName(final String mapName) {
    return mapName.replace(' ', '_').toLowerCase();
  }

  private static List<DownloadFileDescription> filterMaps(final List<DownloadFileDescription> maps,
      final Predicate<DownloadFileDescription> predicate) {
    return maps.stream()
        .peek(map -> checkNotNull(map, "Maps list contained null element: " + maps))
        .filter(predicate)
        .collect(Collectors.toList());
  }

  private JTabbedPane createAvailableInstalledTabbedPanel(
      final Optional<String> selectedMapName,
      final List<DownloadFileDescription> downloads,
      final Set<DownloadFileDescription> pendingDownloads) {
    final MapDownloadList mapList = new MapDownloadList(downloads);

    final JTabbedPane tabbedPane = new JTabbedPane();

    final List<DownloadFileDescription> outOfDateDownloads = mapList.getOutOfDateExcluding(pendingDownloads);
    final JPanel outOfDate = outOfDateDownloads.isEmpty()
        ? null
        : createMapSelectionPanel(selectedMapName, outOfDateDownloads, MapAction.UPDATE);
    // For the UX, always show an available maps tab, even if it is empty
    final JPanel available =
        createMapSelectionPanel(selectedMapName, mapList.getAvailableExcluding(pendingDownloads), MapAction.INSTALL);

    // if there is a map to preselect, show the available map list first
    if (selectedMapName.isPresent()) {
      tabbedPane.addTab("Available", available);
    }

    // otherwise show the updates first
    if (outOfDate != null) {
      tabbedPane.addTab("Update", outOfDate);
    }

    // finally make sure we are always showing the 'available' tab, this condition will be
    // true if the first 'mapName.isPresent()' is false
    if (!selectedMapName.isPresent()) {
      tabbedPane.addTab("Available", available);
    }

    if (!mapList.getInstalled().isEmpty()) {
      final JPanel installed = createMapSelectionPanel(selectedMapName, mapList.getInstalled(), MapAction.REMOVE);
      tabbedPane.addTab("Installed", installed);
    }
    return tabbedPane;
  }

  private JPanel createMapSelectionPanel(final Optional<String> selectedMap,
      final List<DownloadFileDescription> unsortedMaps, final MapAction action) {

    final List<DownloadFileDescription> maps = MapDownloadListSort.sortByMapName(unsortedMaps);
    final JPanel main = JPanelBuilder.builder()
        .borderLayout()
        .borderEmpty(30)
        .build();
    final JEditorPane descriptionPane = SwingComponents.newHtmlJEditorPane();
    main.add(SwingComponents.newJScrollPane(descriptionPane), BorderLayout.CENTER);

    final JLabel mapSizeLabel = new JLabel(" ");

    final DefaultListModel<String> model = SwingComponents.newJListModel(maps, map -> map.getMapName());


    if (maps.size() > 0) {
      final DownloadFileDescription mapToSelect = determineCurrentMapSelection(maps, selectedMap);
      final JList<String> gamesList = createGameSelectionList(mapToSelect, maps, descriptionPane, model);
      gamesList.addListSelectionListener(createDescriptionPanelUpdatingSelectionListener(
          descriptionPane, gamesList, maps, action, mapSizeLabel));

      DownloadMapsWindow.updateMapUrlAndSizeLabel(mapToSelect, action, mapSizeLabel);

      main.add(SwingComponents.newJScrollPane(gamesList), BorderLayout.WEST);
      final JPanel southPanel = JPanelBuilder.builder()
          .gridLayout(2, 1)
          .add(mapSizeLabel)
          .add(createButtonsPanel(action, gamesList, maps, model))
          .build();
      main.add(southPanel, BorderLayout.SOUTH);
    }

    return main;
  }

  private static DownloadFileDescription determineCurrentMapSelection(final List<DownloadFileDescription> maps,
      final Optional<String> mapToSelect) {
    checkArgument(maps.size() > 0);
    if (mapToSelect.isPresent()) {
      final Optional<DownloadFileDescription> potentialMap =
          maps.stream().filter(m -> m.getMapName().equalsIgnoreCase(mapToSelect.get())).findFirst();
      if (potentialMap.isPresent()) {
        return potentialMap.get();
      }
    }

    // just return the first map if nothing selected or could not find one
    return maps.get(0);
  }

  private static JList<String> createGameSelectionList(final DownloadFileDescription selectedMap,
      final List<DownloadFileDescription> maps, final JEditorPane descriptionPanel,
      final DefaultListModel<String> model) {

    final JList<String> gamesList = SwingComponents.newJList(model);
    final int selectedIndex = maps.indexOf(selectedMap);
    gamesList.setSelectedIndex(selectedIndex);

    final String text = maps.get(selectedIndex).toHtmlString();
    descriptionPanel.setText(text);
    descriptionPanel.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
    return gamesList;
  }

  private static ListSelectionListener createDescriptionPanelUpdatingSelectionListener(
      final JEditorPane descriptionPanel,
      final JList<String> gamesList, final List<DownloadFileDescription> maps, final MapAction action,
      final JLabel mapSizeLabelToUpdate) {
    return e -> {
      if (!e.getValueIsAdjusting()) {
        final int index = gamesList.getSelectedIndex();

        final boolean somethingIsSelected = index >= 0;
        if (somethingIsSelected) {
          final String mapName = gamesList.getModel().getElementAt(index);

          // find the map description by map name and update the map download detail panel
          final Optional<DownloadFileDescription> map =
              maps.stream().filter(mapDescription -> mapDescription.getMapName().equals(mapName)).findFirst();
          if (map.isPresent()) {
            final String text = map.get().toHtmlString();
            descriptionPanel.setText(text);
            descriptionPanel.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
            updateMapUrlAndSizeLabel(map.get(), action, mapSizeLabelToUpdate);
          }
        }
      }
    };
  }

  private static void updateMapUrlAndSizeLabel(final DownloadFileDescription map, final MapAction action,
      final JLabel mapSizeLabel) {
    mapSizeLabel.setText(" ");
    new Thread(() -> {
      final String labelText = createLabelText(action, map);
      SwingUtilities.invokeLater(() -> mapSizeLabel.setText(labelText));
    }).start();
  }

  private static String createLabelText(final MapAction action, final DownloadFileDescription map) {
    final String doubleSpace = "&nbsp;&nbsp;";

    final StringBuilder sb = new StringBuilder();
    sb.append("<html>").append(map.getMapName()).append(doubleSpace).append(" v").append(map.getVersion());

    final Optional<Long> mapSize;
    if (action == MapAction.INSTALL) {
      final String mapUrl = map.getUrl();
      mapSize = (mapUrl != null) ? DownloadUtils.getDownloadLength(mapUrl) : Optional.empty();
    } else {
      mapSize = Optional.of(map.getInstallLocation().length());
    }
    mapSize.ifPresent(size -> sb.append(doubleSpace).append(" (").append(createSizeLabel(size)).append(")"));

    sb.append("<br>");

    if (action == MapAction.INSTALL) {
      sb.append(map.getUrl());
    } else {
      sb.append(map.getInstallLocation().getAbsolutePath());
    }

    sb.append("</html>");

    return sb.toString();
  }

  private static String createSizeLabel(final long bytes) {
    final long kiloBytes = (bytes / 1024);
    final long megaBytes = kiloBytes / 1024;
    final long kbDigits = ((kiloBytes % 1000) / 100);
    return megaBytes + "." + kbDigits + " MB";
  }

  private JPanel createButtonsPanel(final MapAction action, final JList<String> gamesList,
      final List<DownloadFileDescription> maps,
      final DefaultListModel<String> listModel) {

    return JPanelBuilder.builder()
        .gridLayout(1, 5)
        .borderEmpty(20)
        .add(buildMapActionButton(action, gamesList, maps, listModel))
        .add(Box.createGlue())
        .add(JButtonBuilder.builder()
            .title("Help")
            .toolTip("Click this button to learn more about the map download feature in TripleA")
            .actionListener(() -> JOptionPane.showMessageDialog(this, new MapDownloadHelpPanel()))
            .build())
        .add(JButtonBuilder.builder()
            .title("Give Map Feedback")
            .toolTip("Click this button to submit map comments and bug reports back to the map makers")
            .actionListener(() -> FeedbackDialog.showFeedbackDialog(gamesList.getSelectedValuesList(), maps))
            .build())
        .add(Box.createGlue())
        .add(JButtonBuilder.builder()
            .title("Close")
            .toolTip("Click this button to close the map download window and cancel any in-progress downloads.")
            .actionListener(() -> {
              setVisible(false);
              dispose();
            })
            .build())
        .build();
  }


  private static final String MULTIPLE_SELECT_MSG =
      "You can select multiple maps by holding control or shift while clicking map names.";

  private JButton buildMapActionButton(final MapAction action, final JList<String> gamesList,
      final List<DownloadFileDescription> maps,
      final DefaultListModel<String> listModel) {
    final JButton actionButton;

    if (action == MapAction.REMOVE) {
      actionButton = JButtonBuilder.builder()
          .title("Remove")
          .toolTip("Click this button to remove the maps selected above from your computer. " + MULTIPLE_SELECT_MSG)
          .actionListener(removeAction(gamesList, maps, listModel))
          .build();
    } else {
      actionButton = JButtonBuilder.builder()
          .title((action == MapAction.INSTALL) ? "Install" : "Update")
          .toolTip("Click this button to download and install the maps selected above. " + MULTIPLE_SELECT_MSG)
          .actionListener(installAction(gamesList, maps, listModel))
          .build();
    }
    return actionButton;
  }

  private static Runnable removeAction(final JList<String> gamesList, final List<DownloadFileDescription> maps,
      final DefaultListModel<String> listModel) {
    return () -> {
      final List<String> selectedValues = gamesList.getSelectedValuesList();
      final List<DownloadFileDescription> selectedMaps =
          maps.stream().filter(map -> selectedValues.contains(map.getMapName()))
              .collect(Collectors.toList());
      if (!selectedMaps.isEmpty()) {
        FileSystemAccessStrategy.remove(selectedMaps, listModel);
      }
    };
  }

  private Runnable installAction(final JList<String> gamesList, final List<DownloadFileDescription> maps,
      final DefaultListModel<String> listModel) {
    return () -> {
      final List<String> selectedValues = gamesList.getSelectedValuesList();
      final List<DownloadFileDescription> downloadList = new ArrayList<>();
      for (final DownloadFileDescription map : maps) {
        if (selectedValues.contains(map.getMapName())) {
          downloadList.add(map);
        }
      }
      if (!downloadList.isEmpty()) {
        progressPanel.download(downloadList);
      }

      downloadList.stream()
          .map(DownloadFileDescription::getMapName)
          .forEach(listModel::removeElement);
    };
  }
}
