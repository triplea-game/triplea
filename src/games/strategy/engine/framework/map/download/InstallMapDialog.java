package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.io.File;
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


  public static void showDownloadMapsWindow(final Component parent, final List<DownloadFileDescription> games) {
    checkNotNull(games);
    final Frame parentFrame = JOptionPane.getFrameForComponent(parent);
    final InstallMapDialog dia = new InstallMapDialog(games);
    dia.setSize(800, WINDOW_HEIGHT);
    dia.setLocationRelativeTo(parentFrame);
    dia.setMinimumSize(new Dimension(200, 200));
    dia.setVisible(true);
    dia.toFront();
  }

  private InstallMapDialog(final List<DownloadFileDescription> games) {
    super("Download Maps");

    progressPanel = new MapDownloadProgressPanel(this);
    SwingComponents.addWindowCloseListener(this, () -> progressPanel.cancel());

    JTabbedPane outerTabs = new JTabbedPane();

    List<DownloadFileDescription> maps = filterMaps(games, download -> download.isMap());
    outerTabs.add("Maps", createAvailableInstalledTabbedPanel(maps));

    List<DownloadFileDescription> mods = filterMaps(games, download -> download.isMapMod());
    outerTabs.add("Mods", createAvailableInstalledTabbedPanel(mods));

    List<DownloadFileDescription> skins = filterMaps(games, download -> download.isMapSkin());
    outerTabs.add("Skins", createAvailableInstalledTabbedPanel(skins));

    List<DownloadFileDescription> tools = filterMaps(games, download -> download.isMapTool());
    outerTabs.add("Tools", createAvailableInstalledTabbedPanel(tools));

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outerTabs,
        SwingComponents.newJScrollPane(progressPanel));
    splitPane.setDividerLocation(DIVIDER_POSITION);
    add(splitPane);
  }

  private static List<DownloadFileDescription> filterMaps(List<DownloadFileDescription> maps,
      Function<DownloadFileDescription, Boolean> filter) {

    maps.forEach(map -> checkNotNull("Maps list contained null element: " + maps, map));
    return maps.stream().filter(map -> filter.apply(map)).collect(Collectors.toList());
  }

  private JTabbedPane createAvailableInstalledTabbedPanel(final List<DownloadFileDescription> games) {
    MapDownloadList mapList = new MapDownloadList(games, new FileSystemAccessStrategy());

    JTabbedPane tabbedPane = new JTabbedPane();

    if (containsMaps(mapList.getOutOfDate())) {
      final JPanel outOfDate = createMapSelectionPanel(mapList.getOutOfDate(), MapAction.UPDATE);
      tabbedPane.addTab("Update", outOfDate);
    }

    // For the UX, always show an available maps tab, even if it is empty
    final JPanel available = createMapSelectionPanel(mapList.getAvailable(), MapAction.INSTALL);
    tabbedPane.addTab("Available", available);

    if (containsMaps(mapList.getInstalled())) {
      final JPanel installed = createMapSelectionPanel(mapList.getInstalled(), MapAction.REMOVE);
      tabbedPane.addTab("Installed", installed);
    }
    return tabbedPane;
  }


  private static boolean containsMaps(List<DownloadFileDescription> maps) {
    return maps.stream().anyMatch(e -> !e.isDummyUrl());
  }

  private JPanel createMapSelectionPanel(List<DownloadFileDescription> unsortedMaps, MapAction action) {
    final List<DownloadFileDescription> maps = MapDownloadListSort.sortByMapName(unsortedMaps);
    final JPanel main = SwingComponents.newBorderedPanel(30);
    final JEditorPane descriptionPane = SwingComponents.newHtmlJEditorPane();
    main.add(SwingComponents.newJScrollPane(descriptionPane), BorderLayout.CENTER);

    final JLabel mapSizeLabel = new JLabel(" ");

    final DefaultListModel listModel = createGameSelectionListModel(maps);
    final JList<String> gamesList = createGameSelectionList(listModel, maps, descriptionPane);
    gamesList.addListSelectionListener(createDescriptionPanelUpdatingSelectionListener(
        descriptionPane, gamesList, maps, action, mapSizeLabel));
    main.add(SwingComponents.newJScrollPane(gamesList), BorderLayout.WEST);

    JPanel southPanel = SwingComponents.gridPanel(2, 1);
    southPanel.add(mapSizeLabel);
    southPanel.add(createButtonsPanel(action, gamesList, maps, listModel));
    main.add(southPanel, BorderLayout.SOUTH);

    return main;
  }


  private static DefaultListModel createGameSelectionListModel(List<DownloadFileDescription> maps) {
    return SwingComponents.newJListModel(maps, (map) -> map.getMapName());
  }


  private static JList<String> createGameSelectionList(DefaultListModel model, List<DownloadFileDescription> maps,
      JEditorPane descriptionPanel) {
    JList gamesList = SwingComponents.newJList(model);

    Optional<Integer> index = getDefaultSelectionIndex(maps);
    if (index.isPresent()) {
      gamesList.setSelectedIndex(index.get());
      String text = createEditorPaneText(maps.get(index.get()));
      descriptionPanel.setText(text);
    }
    return gamesList;
  }

  private static Optional<Integer> getDefaultSelectionIndex(List<DownloadFileDescription> maps) {
    // select the first map, not header
    for (int i = 0; i < maps.size(); i++) {
      if (!maps.get(i).isDummyUrl()) {
        return Optional.of(i);
      }
    }
    return Optional.empty();
  }



  private static ListSelectionListener createDescriptionPanelUpdatingSelectionListener(JEditorPane descriptionPanel,
      JList<String> gamesList, List<DownloadFileDescription> maps, MapAction action, JLabel mapSizeLabel) {
    return e -> {
      final int index = gamesList.getSelectedIndex();
      if (index > -1) {
        DownloadFileDescription map = maps.get(index);

        String text = createEditorPaneText(map);
        descriptionPanel.setText(text);
        descriptionPanel.scrollRectToVisible(new Rectangle(0, 0, 0, 0));

        mapSizeLabel.setText(" ");
        if (!map.isDummyUrl()) {
          (new Thread(() -> {
            final String labelText = createLabelText(action, map);
            if (index == gamesList.getSelectedIndex()) {
              SwingUtilities.invokeLater(() -> mapSizeLabel.setText(labelText));
            }
          })).start();
        }

      }
    };
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


  private JButton buildMapActionButton(MapAction action, JList<String> gamesList, List<DownloadFileDescription> maps,
      DefaultListModel listModel) {
    final JButton actionButton;

    if (action == MapAction.REMOVE) {
      // We close the window after removing maps, so we do not need to pass in the listModel which would otherwise be used
      // to update the JList to no longer contain the map we  are removing.
      actionButton = SwingComponents.newJButton("Remove", removeAction(gamesList, maps));
    } else {
      final String buttonText;
      if (action == MapAction.INSTALL) {
        buttonText = "Install";
      } else {
        buttonText = "Update";
      }
      actionButton = SwingComponents.newJButton(buttonText, installAction(gamesList, maps, listModel));
      actionButton.setToolTipText(
          "Click this button to install the currently selected map(s). Click the map names above while holding control to select multiple maps for installation.");
    }
    return actionButton;
  }

  private ActionListener removeAction(JList<String> gamesList, List<DownloadFileDescription> maps) {
    return (e) -> {
      final List<String> selectedValues = gamesList.getSelectedValuesList();
      final List<DownloadFileDescription> selectedMaps =
          maps.stream().filter(map -> !map.isDummyUrl() && selectedValues.contains(map.getMapName()))
              .collect(Collectors.toList());

      final Runnable removeCompleteCallback = () -> {
        setVisible(false);
        dispose();
      };
      FileSystemAccessStrategy.remove(selectedMaps, removeCompleteCallback);
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
