package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
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

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.ui.SwingComponents;
import games.strategy.util.Version;


/** Window that allows for map downloads and removal */
public class DownloadMapsWindow extends JFrame {
  private enum MapAction {
    INSTALL, UPDATE, REMOVE
  }

  private static final long serialVersionUID = -1542210716764178580L;
  private static final int WINDOW_WIDTH = 1200;
  private static final int WINDOW_HEIGHT = 700;
  private static final int DIVIDER_POSITION = WINDOW_HEIGHT - 150;

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
    showDownloadMapsWindow(Optional.of(mapName));
  }

  public static void showDownloadMapsWindow() {
    showDownloadMapsWindow(Optional.empty());
  }


  private static void showDownloadMapsWindow(Optional<String> mapName) {
    Runnable downloadAndShowWindow = () -> {
      final List<DownloadFileDescription> games = new DownloadRunnable(ClientContext.mapListingSource().getMapListDownloadSite()).getDownloads();
      checkNotNull(games);

      SwingUtilities.invokeLater(() -> {
        final DownloadMapsWindow dia = new DownloadMapsWindow(mapName, games);
        dia.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        dia.setLocationRelativeTo(null);
        dia.setMinimumSize(new Dimension(200, 200));
        dia.setVisible(true);
        dia.requestFocus();
        dia.toFront();
      });
    };
    final String popupWindowTitle = "Downloading list of availabe maps....";
    BackgroundTaskRunner.runInBackground(popupWindowTitle, downloadAndShowWindow);
  }

  private DownloadMapsWindow(final Optional<String> mapName, final List<DownloadFileDescription> games) {
    super("Download Maps");
    setAlwaysOnTop(true);
    setIconImage(GameRunner.getGameIcon(this));
    progressPanel = new MapDownloadProgressPanel(this);
    if (mapName.isPresent()) {
      final Optional<DownloadFileDescription> mapDownload = findMap(mapName.get(), games);
      if (mapDownload.isPresent()) {
        progressPanel.download(Arrays.asList(mapDownload.get()));
      } else {
        SwingComponents.newMessageDialog("Unable to download map, could not find: " + mapName.get());
      }
    }
    SwingComponents.addWindowCloseListener(this, () -> progressPanel.cancel());

    final JTabbedPane outerTabs = new JTabbedPane();

    final List<DownloadFileDescription> maps = filterMaps(games, download -> download.isMap());
    outerTabs.add("Maps", createdTabbedPanelForMaps(maps));

    final List<DownloadFileDescription> mods = filterMaps(games, download -> download.isMapMod());
    outerTabs.add("Mods", createAvailableInstalledTabbedPanel(mapName, mods));

    final List<DownloadFileDescription> skins = filterMaps(games, download -> download.isMapSkin());
    outerTabs.add("Skins", createAvailableInstalledTabbedPanel(mapName, skins));

    final List<DownloadFileDescription> tools = filterMaps(games, download -> download.isMapTool());
    outerTabs.add("Tools", createAvailableInstalledTabbedPanel(mapName, tools));

    final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outerTabs,
        SwingComponents.newJScrollPane(progressPanel));
    splitPane.setDividerLocation(DIVIDER_POSITION);
    add(splitPane);
  }

  private Component createdTabbedPanelForMaps(List<DownloadFileDescription> maps) {

    JTabbedPane mapTabs = SwingComponents.newJTabbedPane();

    for(DownloadFileDescription.MapCategory mapCategory : Arrays.asList(DownloadFileDescription.MapCategory.values()) ) {

      List<DownloadFileDescription> mapsByCategory =
          maps.stream().filter(map -> map.getMapCategory() == mapCategory).collect(Collectors.toList());
      JTabbedPane subTab = createAvailableInstalledTabbedPanel(Optional.of(mapCategory.toString()), mapsByCategory);
      mapTabs.add(mapCategory.toString(), subTab);
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
      final Function<DownloadFileDescription, Boolean> filter) {

    maps.forEach(map -> checkNotNull("Maps list contained null element: " + maps, map));
    return maps.stream().filter(map -> filter.apply(map)).collect(Collectors.toList());
  }

  private JTabbedPane createAvailableInstalledTabbedPanel(final Optional<String> mapName,
      final List<DownloadFileDescription> games) {
    final MapDownloadList mapList = new MapDownloadList(games, new FileSystemAccessStrategy());

    final JTabbedPane tabbedPane = new JTabbedPane();


    JPanel outOfDate = null;
    if (!mapList.getOutOfDate().isEmpty()) {
      outOfDate = createMapSelectionPanel(mapName, mapList.getOutOfDate(), MapAction.UPDATE);
    }
    // For the UX, always show an available maps tab, even if it is empty
    final JPanel available = createMapSelectionPanel(mapName, mapList.getAvailable(), MapAction.INSTALL);


    // if there is a map to preselect, show the available map list first
    if (mapName.isPresent()) {
      tabbedPane.addTab("Available", available);
    }

    // otherwise show the updates first
    if (outOfDate != null) {
      tabbedPane.addTab("Update", outOfDate);
    }

    // finally make sure we are always showing the 'available' tab, this condition will be
    // true if the first 'mapName.isPresent()' is false
    if (!mapName.isPresent()) {
      tabbedPane.addTab("Available", available);
    }

    if (!mapList.getInstalled().isEmpty()) {
      final JPanel installed = createMapSelectionPanel(mapName, mapList.getInstalled(), MapAction.REMOVE);
      tabbedPane.addTab("Installed", installed);
    }
    return tabbedPane;
  }

  private JPanel createMapSelectionPanel(final Optional<String> selectedMap,
      final List<DownloadFileDescription> unsortedMaps, final MapAction action) {

    final List<DownloadFileDescription> maps = MapDownloadListSort.sortByMapName(unsortedMaps);
    final JPanel main = SwingComponents.newBorderedPanel(30);
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
      final JPanel southPanel = SwingComponents.gridPanel(2, 1);
      southPanel.add(mapSizeLabel);
      southPanel.add(createButtonsPanel(action, gamesList, maps, model));
      main.add(southPanel, BorderLayout.SOUTH);
    }

    return main;
  }

  private DownloadFileDescription determineCurrentMapSelection(final List<DownloadFileDescription> maps,
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
    final String DOUBLE_SPACE = "&nbsp;&nbsp;";

    final long mapSize;
    String labelText = "<html>" + map.getMapName() + DOUBLE_SPACE + " v" + map.getVersion() + DOUBLE_SPACE + " (";
    if (action == MapAction.INSTALL) {
      if(map.newURL() == null ) {
       mapSize = 0L;
      } else {
        mapSize = DownloadUtils.getDownloadLength(map.newURL()).orElse(-1);
      }
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

  private static String createSizeLabel(final long bytes) {
    final long kiloBytes = (bytes / 1024);
    final long megaBytes = kiloBytes / 1024;
    final long kbDigits = ((kiloBytes % 1000) / 100);
    return megaBytes + "." + kbDigits + " MB";
  }

  private JPanel createButtonsPanel(final MapAction action, final JList<String> gamesList,
      final List<DownloadFileDescription> maps,
      final DefaultListModel<String> listModel) {
    final JPanel buttonsPanel = SwingComponents.gridPanel(1, 5);

    buttonsPanel.setBorder(SwingComponents.newEmptyBorder(20));


    buttonsPanel.add(buildMapActionButton(action, gamesList, maps, listModel));

    buttonsPanel.add(Box.createGlue());

    String toolTip = "Click this button to learn more about the map download feature in TripleA";
    final JButton helpButton = SwingComponents.newJButton("Help", toolTip,
        e -> JOptionPane.showMessageDialog(this, new MapDownloadHelpPanel()));
    buttonsPanel.add(helpButton);

    toolTip = "Click this button to submit map comments and bug reports back to the map makers";
    final JButton mapFeedbackButton = SwingComponents.newJButton("Give Map Feedback", toolTip,
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

  private JButton buildMapActionButton(final MapAction action, final JList<String> gamesList,
      final List<DownloadFileDescription> maps,
      final DefaultListModel<String> listModel) {
    final JButton actionButton;

    if (action == MapAction.REMOVE) {
      actionButton = SwingComponents.newJButton("Remove", removeAction(gamesList, maps, listModel));

      final String hoverText =
          "Click this button to remove the maps selected above from your computer. " + MULTIPLE_SELECT_MSG;
      actionButton.setToolTipText(hoverText);
    } else {
      final String buttonText = (action == MapAction.INSTALL) ? "Install" : "Update";
      actionButton = SwingComponents.newJButton(buttonText, installAction(gamesList, maps, listModel));
      final String hoverText =
          "Click this button to download and install the maps selected above. " + MULTIPLE_SELECT_MSG;
      actionButton.setToolTipText(hoverText);
    }
    return actionButton;
  }

  private static ActionListener removeAction(final JList<String> gamesList, final List<DownloadFileDescription> maps,
      final DefaultListModel<String> listModel) {
    return (e) -> {
      final List<String> selectedValues = gamesList.getSelectedValuesList();
      final List<DownloadFileDescription> selectedMaps =
          maps.stream().filter(map -> selectedValues.contains(map.getMapName()))
              .collect(Collectors.toList());
      if (!selectedMaps.isEmpty()) {
        FileSystemAccessStrategy.remove(selectedMaps, listModel);
      }
    };
  }

  private ActionListener installAction(final JList<String> gamesList, final List<DownloadFileDescription> maps,
      final DefaultListModel<String> listModel) {
    return (e) -> {
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

      downloadList.forEach(m -> listModel.removeElement(m.getMapName()));
    };
  }
}
