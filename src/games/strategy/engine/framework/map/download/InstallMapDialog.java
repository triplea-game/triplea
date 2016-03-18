package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
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

import com.google.common.collect.Lists;

import games.strategy.common.swing.SwingComponents;
import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.util.Version;


// TODO: rename to DownloadMapswindow
/** Window that allows for map downloads and removal */
public class InstallMapDialog extends JFrame {
  private static final long serialVersionUID = -1542210716764178580L;

  private static enum MapAction {
    INSTALL, UPDATE, REMOVE
  }

  private static final int WINDOW_HEIGHT = 670;
  private static final int DIVIDER_POSITION = WINDOW_HEIGHT - 100;

  private final MapDownloadProgressPanel progressPanel;

  public static Version getVersion(final File zipFile) {
    final DownloadFileProperties props = DownloadFileProperties.loadForZip(zipFile);
    return props.getVersion();
  }

  /**
   * Shows the download window and begins downloading the specified map right away.
   * If the map cannot be downloaded a message prompt is shown to the user.
   */
  public static void showDownloadMapsWindow(final String mapName) {
    showDownloadMapsWindow(null, Optional.of(mapName));
  }

  public static void showDownloadMapsWindow(final Component parent) {
    showDownloadMapsWindow(parent, Optional.empty());
  }

  public static void showDownloadMapsWindow() {
    showDownloadMapsWindow(null, Optional.empty());
  }


  private static void showDownloadMapsWindow(final Component parent, Optional<String> mapName) {
    final DownloadRunnable download = new DownloadRunnable(ClientContext.mapListingSource().getMapListDownloadSite());
    final String popupWindowTitle = "Downloading list of availabe maps....";
    BackgroundTaskRunner.runInBackground(null, popupWindowTitle, download);
    final List<DownloadFileDescription> games = download.getDownloads();
    checkNotNull(games);

    final Frame parentFrame = JOptionPane.getFrameForComponent(parent);
    final InstallMapDialog dia = new InstallMapDialog(mapName, games);
    dia.setSize(800, WINDOW_HEIGHT);
    dia.setLocationRelativeTo(parentFrame);
    dia.setMinimumSize(new Dimension(200, 200));
    dia.setVisible(true);
  }

  private InstallMapDialog(final Optional<String> mapName, final List<DownloadFileDescription> games) {
    super("Download Maps");

    progressPanel = new MapDownloadProgressPanel(this);
    if (mapName.isPresent()) {
      Optional<DownloadFileDescription> mapDownload = findMap(mapName.get(), games);
      if (mapDownload.isPresent()) {
        progressPanel.download(Arrays.asList(mapDownload.get()));
      } else {
        SwingComponents.newMessageDialog("Unable to download map, could not find: " + mapName.get());
      }
    }
    SwingComponents.addWindowCloseListener(this, () -> progressPanel.cancel());

    JTabbedPane outerTabs = new JTabbedPane();

    List<DownloadFileDescription> maps = filterMaps(games, download -> download.isMap());
    outerTabs.add("Maps", createAvailableInstalledTabbedPanel(mapName, maps));

    List<DownloadFileDescription> mods = filterMaps(games, download -> download.isMapMod());
    outerTabs.add("Mods", createAvailableInstalledTabbedPanel(mapName, mods));

    List<DownloadFileDescription> skins = filterMaps(games, download -> download.isMapSkin());
    outerTabs.add("Skins", createAvailableInstalledTabbedPanel(mapName, skins));

    List<DownloadFileDescription> tools = filterMaps(games, download -> download.isMapTool());
    outerTabs.add("Tools", createAvailableInstalledTabbedPanel(mapName, tools));

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outerTabs,
        SwingComponents.newJScrollPane(progressPanel));
    splitPane.setDividerLocation(DIVIDER_POSITION);
    add(splitPane);
  }

  private static Optional<DownloadFileDescription> findMap(final String mapName,
      final List<DownloadFileDescription> games) {
    for (DownloadFileDescription download : games) {
      if (download.getMapName().equalsIgnoreCase(mapName)) {
        return Optional.of(download);
      }
    }
    return Optional.empty();
  }


  private static List<DownloadFileDescription> filterMaps(final List<DownloadFileDescription> maps,
      final Function<DownloadFileDescription, Boolean> filter) {

    maps.forEach(map -> checkNotNull("Maps list contained null element: " + maps, map));
    return maps.stream().filter(map -> filter.apply(map)).collect(Collectors.toList());
  }

  private JTabbedPane createAvailableInstalledTabbedPanel(final Optional<String> mapName,
      final List<DownloadFileDescription> games) {
    MapDownloadList mapList = new MapDownloadList(games, new FileSystemAccessStrategy());

    JTabbedPane tabbedPane = new JTabbedPane();


    JPanel outOfDate = null;
    if (containsNonDummyMaps(mapList.getOutOfDate())) {
      outOfDate = createMapSelectionPanel(mapName, mapList.getOutOfDate(), MapAction.UPDATE);
    }
    // For the UX, always show an available maps tab, even if it is empty
    final JPanel available = createMapSelectionPanel(mapName, mapList.getAvailable(), MapAction.INSTALL);


    // if there is a map to preselect, show the available map list first
    if (mapName.isPresent()) {
      tabbedPane.addTab("Available", available);
    }

    // otherwise show the updates first
    if( outOfDate != null ) {
      tabbedPane.addTab("Update", outOfDate);
    }

    // finally make sure we are always showing the 'available' tab, this condition will be
    // true if the first 'mapName.isPresent()' is false
    if (!mapName.isPresent()) {
      tabbedPane.addTab("Available", available);
    }

    if (containsNonDummyMaps(mapList.getInstalled())) {
      final JPanel installed = createMapSelectionPanel(mapName, mapList.getInstalled(), MapAction.REMOVE);
      tabbedPane.addTab("Installed", installed);
    }
    return tabbedPane;
  }


  private static boolean containsNonDummyMaps(List<DownloadFileDescription> maps) {
    return maps.stream().anyMatch(e -> !e.isDummyUrl());
  }

  private JPanel createMapSelectionPanel(final Optional<String> selectedMap,
      final List<DownloadFileDescription> unsortedMaps,
      final MapAction action) {
    final List<DownloadFileDescription> maps = MapDownloadListSort.sortByMapName(unsortedMaps);
    final JPanel main = SwingComponents.newBorderedPanel(30);
    final JEditorPane descriptionPane = SwingComponents.newHtmlJEditorPane();
    main.add(SwingComponents.newJScrollPane(descriptionPane), BorderLayout.CENTER);

    final JLabel mapSizeLabel = new JLabel(" ");

    DefaultListModel model = SwingComponents.newJListModel(maps, (map) -> map.getMapName());

    final JList<String> gamesList = createGameSelectionList(selectedMap, maps, descriptionPane, model);
    gamesList.addListSelectionListener(createDescriptionPanelUpdatingSelectionListener(
        descriptionPane, gamesList, maps, action, mapSizeLabel));
    main.add(SwingComponents.newJScrollPane(gamesList), BorderLayout.WEST);

    final JPanel southPanel = SwingComponents.gridPanel(2, 1);
    southPanel.add(mapSizeLabel);
    southPanel.add(createButtonsPanel(action, gamesList, maps, model));
    main.add(southPanel, BorderLayout.SOUTH);

    return main;
  }

  private static JList<String> createGameSelectionList(final Optional<String> selectedMap,
      final List<DownloadFileDescription> maps, final JEditorPane descriptionPanel, DefaultListModel model) {

    JList<String> gamesList = SwingComponents.newJList(model);
    // select the first map, not header
    int selectedIndex = 0;
    for (int i = 0; i < maps.size(); i++) {
      final DownloadFileDescription currentMap = maps.get(i);
      final boolean selectedByMapName = selectedMap.isPresent() && selectedMap.get().equals(currentMap.getMapName());
      final boolean selectedByFirstNonDummyUrl = !selectedMap.isPresent() && !currentMap.isDummyUrl();
      if (selectedByMapName || selectedByFirstNonDummyUrl) {
        selectedIndex = i;
        final String text = createEditorPaneText(maps.get(i));
        descriptionPanel.setText(text);
        descriptionPanel.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
        break;
      }
    }
    gamesList.setSelectedIndex(selectedIndex);
    return gamesList;
  }

  private static ListSelectionListener createDescriptionPanelUpdatingSelectionListener(JEditorPane descriptionPanel,
      JList<String> gamesList, List<DownloadFileDescription> maps, MapAction action, JLabel mapSizeLabel) {
    return e -> {
      final int index = gamesList.getSelectedIndex();
      if( index > 0 ) {
        DownloadFileDescription map = maps.get(index);

        String text = createEditorPaneText(map);
        descriptionPanel.setText(text);
        descriptionPanel.scrollRectToVisible(new Rectangle(0, 0, 0, 0));

        updateMapUrlAndSizeLabel(map, action, mapSizeLabel);
      }
    };
  }

  private static void updateMapUrlAndSizeLabel(DownloadFileDescription map, MapAction action, JLabel mapSizeLabel) {
    mapSizeLabel.setText(" ");
    if (!map.isDummyUrl()) {
      (new Thread(() -> {
        final String labelText = createLabelText(action, map);
        SwingUtilities.invokeLater(() -> mapSizeLabel.setText(labelText));
      })).start();
    }

  }

  private static String createLabelText(MapAction action, DownloadFileDescription map) {
    final String DOUBLE_SPACE = "&nbsp;&nbsp;";

    final long mapSize;
    String labelText = "<html>" + map.getMapName() + DOUBLE_SPACE + " v" + map.getVersion() + DOUBLE_SPACE + " (";
    if (action == MapAction.INSTALL) {
      mapSize = DownloadUtils.getDownloadLength(map.newURL());
    } else {
      mapSize = map.getInstallLocation().length();
    }
    labelText += createSizeLabel(mapSize) + ")<br/>";

    if (action == MapAction.INSTALL) {
      labelText += map.getUrl();
    } else {
      labelText += map.getInstallLocation().getAbsolutePath();
    }

    labelText += "</html>";
    return labelText;
  }

  private static String createSizeLabel(long bytes) {
    long kiloBytes = (bytes / 1024);
    long megaBytes = kiloBytes / 1024;
    long kbDigits = ((kiloBytes % 1000) / 100);
    return megaBytes + "." + kbDigits + " MB";
  }

  private static String createEditorPaneText(DownloadFileDescription map) {
    String text = "<h2>" + map.getMapName() + "</h2>";
    if (map.isDummyUrl()) {
      text += map.getDescription();
    } else {
      text += map.getDescription();
    }
    return text;
  }

  private JPanel createButtonsPanel(MapAction action, JList<String> gamesList, List<DownloadFileDescription> maps,
      DefaultListModel listModel) {
    final JPanel buttonsPanel = SwingComponents.gridPanel(1, 5);

    buttonsPanel.setBorder(SwingComponents.newEmptyBorder(20));


    buttonsPanel.add(buildMapActionButton(action, gamesList, maps, listModel));

    buttonsPanel.add(Box.createGlue());

    String toolTip = "Click this button to learn more about the map download feature in TripleA";
    JButton helpButton = SwingComponents.newJButton("Help", toolTip,
        e -> JOptionPane.showMessageDialog(this, new MapDownloadHelpPanel()));
    buttonsPanel.add(helpButton);

    toolTip = "Click this button to submit map comments and bug reports back to the map makers";
    JButton mapFeedbackButton = SwingComponents.newJButton("Feedback", toolTip,
        e -> FeedbackDialog.showFeedbackDialog(gamesList.getSelectedValuesList(), maps));
    buttonsPanel.add(mapFeedbackButton);

    buttonsPanel.add(Box.createGlue());

    toolTip = "Click this button to close the map download window and cancel any in-progress downloads.";
    final JButton closeButton = SwingComponents.newJButton("Close", toolTip, e -> {
      setVisible(false);
      dispose();
    });
    buttonsPanel.add(closeButton);

    return buttonsPanel;
  }


  private static final String MULTIPLE_SELECT_MSG =
      "You can select multiple maps by holding control or shift while clicking map names.";

  private JButton buildMapActionButton(MapAction action, JList<String> gamesList, List<DownloadFileDescription> maps,
      DefaultListModel listModel) {
    final JButton actionButton;

    if (action == MapAction.REMOVE) {
      actionButton = SwingComponents.newJButton("Remove", removeAction(gamesList, maps, listModel));

      String hoverText =
          "Click this button to remove the maps selected above from your computer. " + MULTIPLE_SELECT_MSG;
      actionButton.setToolTipText(hoverText);
    } else {
      final String buttonText = (action == MapAction.INSTALL) ? "Install" : "Update";
      actionButton = SwingComponents.newJButton(buttonText, installAction(gamesList, maps, listModel));
      String hoverText = "Click this button to download and install the maps selected above. " + MULTIPLE_SELECT_MSG;
      actionButton.setToolTipText(hoverText);
    }
    return actionButton;
  }

  private static ActionListener removeAction(JList<String> gamesList, List<DownloadFileDescription> maps,
      DefaultListModel listModel) {
    return (e) -> {
      final List<String> selectedValues = gamesList.getSelectedValuesList();
      final List<DownloadFileDescription> selectedMaps =
          maps.stream().filter(map -> !map.isDummyUrl() && selectedValues.contains(map.getMapName()))
              .collect(Collectors.toList());
      if (!selectedMaps.isEmpty()) {
        FileSystemAccessStrategy.remove(selectedMaps, listModel);
      }
    };
  }

  private ActionListener installAction(JList gamesList, List<DownloadFileDescription> maps,
      DefaultListModel listModel) {
    return (e) -> {
      List<String> selectedValues = gamesList.getSelectedValuesList();
      List<DownloadFileDescription> downloadList = Lists.newArrayList();
      for (DownloadFileDescription map : maps) {
        if (selectedValues.contains(map.getMapName())) {
          downloadList.add(map);
        }
      }
      if (!downloadList.isEmpty()) {
        progressPanel.download(downloadList);
      }

      downloadList.forEach(m -> {
        if (!m.isDummyUrl()) {
          listModel.removeElement(m.getMapName());
        }
      });
    };
  }
}
