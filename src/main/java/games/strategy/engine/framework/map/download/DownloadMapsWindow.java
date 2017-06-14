package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.ui.SwingComponents;
import games.strategy.util.OptionalUtils;

/** Window that allows for map downloads and removal. */
public class DownloadMapsWindow extends JFrame {
  private enum MapAction {
    INSTALL, UPDATE, REMOVE
  }

  private static final long serialVersionUID = -1542210716764178580L;
  private static final int WINDOW_WIDTH = 1200;
  private static final int WINDOW_HEIGHT = 700;
  private static final int DIVIDER_POSITION = WINDOW_HEIGHT - 150;

  private final MapDownloadProgressPanel progressPanel;

  public static void showDownloadMapsWindow() {
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
   */
  public static void showDownloadMapsWindowAndDownload(final String mapName) {
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
   */
  public static void showDownloadMapsWindowAndDownload(final Collection<String> mapNames) {
    checkNotNull(mapNames);

    final Runnable downloadAndShowWindow = () -> {
      final List<DownloadFileDescription> allDownloads =
          new DownloadRunnable(ClientContext.mapListingSource().getMapListDownloadSite()).getDownloads();
      checkNotNull(allDownloads);

      SwingUtilities.invokeLater(() -> {
        final DownloadMapsWindow dia = new DownloadMapsWindow(mapNames, allDownloads);
        dia.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        dia.setLocationRelativeTo(null);
        dia.setMinimumSize(new Dimension(200, 200));
        dia.setVisible(true);
        dia.requestFocus();
        dia.toFront();
      });
    };
    final String popupWindowTitle = "Downloading list of available maps...";
    BackgroundTaskRunner.runInBackground(popupWindowTitle, downloadAndShowWindow);
  }

  private DownloadMapsWindow(
      final Collection<String> pendingDownloadMapNames,
      final List<DownloadFileDescription> allDownloads) {
    super("Download Maps");
    setIconImage(GameRunner.getGameIcon(this));
    progressPanel = new MapDownloadProgressPanel();

    final List<DownloadFileDescription> pendingDownloads = new ArrayList<>();
    final Collection<String> unknownMapNames = new ArrayList<>();
    for (final String mapName : pendingDownloadMapNames) {
      OptionalUtils.ifPresentOrElse(findMap(mapName, allDownloads),
          pendingDownloads::add,
          () -> unknownMapNames.add(mapName));
    }

    if (!pendingDownloads.isEmpty()) {
      progressPanel.download(pendingDownloads);
    }
    // TODO: there is a possibility that pendingDownloads will contain duplicates after the following call.
    // i don't think it matters, but we should try to avoid it if possible. might be as simply as changing
    // to a Set.
    pendingDownloads.addAll(DownloadCoordinator.INSTANCE.getPendingDownloads().stream()
        .map(DownloadFile::getDownload)
        .collect(Collectors.toList()));
    pendingDownloads.addAll(DownloadCoordinator.INSTANCE.getActiveDownloads().stream()
        .map(DownloadFile::getDownload)
        .collect(Collectors.toList()));

    if (!unknownMapNames.isEmpty()) {
      SwingComponents.newMessageDialog(formatUnknownPendingMapsMessage(unknownMapNames));
    }

    final Optional<String> selectedMapName = pendingDownloadMapNames.stream().findFirst();

    SwingComponents.addWindowCloseListener(this, () -> progressPanel.cancel());

    final JTabbedPane outerTabs = new JTabbedPane();

    final List<DownloadFileDescription> maps = filterMaps(allDownloads, download -> download.isMap());
    outerTabs.add("Maps", createdTabbedPanelForMaps(maps, pendingDownloads));

    final List<DownloadFileDescription> skins = filterMaps(allDownloads, download -> download.isMapSkin());
    outerTabs.add("Skins", createAvailableInstalledTabbedPanel(selectedMapName, skins, pendingDownloads));

    final List<DownloadFileDescription> tools = filterMaps(allDownloads, download -> download.isMapTool());
    outerTabs.add("Tools", createAvailableInstalledTabbedPanel(selectedMapName, tools, pendingDownloads));

    final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outerTabs,
        SwingComponents.newJScrollPane(progressPanel));
    splitPane.setDividerLocation(DIVIDER_POSITION);
    add(splitPane);
  }

  private static String formatUnknownPendingMapsMessage(final Collection<String> mapNames) {
    final StringBuilder sb = new StringBuilder();
    sb.append("<html>");
    sb.append("Unable to download map(s).<br>");
    sb.append("<br>");
    sb.append("Could not find the following map(s):<br>");
    sb.append("<ul>");
    for (final String mapName : mapNames) {
      sb.append("<li>").append(mapName).append("</li>");
    }
    sb.append("</ul>");
    sb.append("</html>");
    return sb.toString();
  }

  private Component createdTabbedPanelForMaps(
      final List<DownloadFileDescription> downloads,
      final List<DownloadFileDescription> pendingDownloads) {
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
      final Function<DownloadFileDescription, Boolean> filter) {

    maps.forEach(map -> checkNotNull("Maps list contained null element: " + maps, map));
    return maps.stream().filter(map -> filter.apply(map)).collect(Collectors.toList());
  }

  private JTabbedPane createAvailableInstalledTabbedPanel(
      final Optional<String> selectedMapName,
      final List<DownloadFileDescription> downloads,
      final List<DownloadFileDescription> pendingDownloads) {
    final MapDownloadList mapList = new MapDownloadList(downloads, new FileSystemAccessStrategy());

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
    return e -> {
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
