package games.strategy.triplea.ui.menubar;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.export.GameDataExporter;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.printgenerator.SetupFrame;
import games.strategy.triplea.ui.ExtendedStats;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.export.ScreenshotExporter;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.triplea.util.PlayerOrderComparator;
import games.strategy.ui.SwingAction;
import games.strategy.util.FileNameUtils;
import games.strategy.util.LocalizeHtml;

class ExportMenu {

  private final TripleAFrame frame;
  private final GameData gameData;
  private final UiContext uiContext;
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");

  ExportMenu(final TripleAMenuBar menuBar, final TripleAFrame frame) {
    this.frame = frame;
    gameData = frame.getGame().getData();
    uiContext = frame.getUiContext();

    final JMenu menuGame = new JMenu("Export");
    menuGame.setMnemonic(KeyEvent.VK_E);
    menuBar.add(menuGame);
    addExportXml(menuGame);
    addExportStats(menuGame);
    addExportStatsFull(menuGame);
    addExportSetupCharts(menuGame);
    addExportUnitStats(menuGame);
    addSaveScreenshot(menuGame);
  }

  // TODO: create a second menu option for parsing current attachments
  private void addExportXml(final JMenu parentMenu) {
    final Action exportXml = SwingAction.of("Export game.xml File (Beta)", e -> exportXmlFile());
    parentMenu.add(exportXml).setMnemonic(KeyEvent.VK_X);
  }

  private void exportXmlFile() {
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    final File rootDir = new File(SystemProperties.getUserDir());

    final int round = gameData.getCurrentRound();
    String defaultFileName =
        "xml_" + dateTimeFormatter.format(LocalDateTime.now()) + "_" + gameData.getGameName() + "_round_" + round;
    defaultFileName = FileNameUtils.removeIllegalCharacters(defaultFileName);
    defaultFileName = defaultFileName + ".xml";
    chooser.setSelectedFile(new File(rootDir, defaultFileName));
    if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
      return;
    }
    final String xmlFile;
    try {
      gameData.acquireReadLock();
      final GameDataExporter exporter = new GameDataExporter(gameData);
      xmlFile = exporter.getXml();
    } finally {
      gameData.releaseReadLock();
    }
    try (Writer writer = new FileWriter(chooser.getSelectedFile())) {
      writer.write(xmlFile);
    } catch (final IOException e1) {
      ClientLogger.logQuietly("Failed to write XML: " + chooser.getSelectedFile().getAbsolutePath(), e1);
    }
  }


  private void addSaveScreenshot(final JMenu parentMenu) {
    final Action abstractAction = SwingAction.of("Export Map Snapshot", e -> {
      // get current history node. if we are in history view, get the selected node.
      final HistoryPanel historyPanel = frame.getHistoryPanel();
      final HistoryNode curNode;
      if (historyPanel == null) {
        curNode = gameData.getHistory().getLastNode();
      } else {
        curNode = historyPanel.getCurrentNode();
      }
      ScreenshotExporter.exportScreenshot(frame, gameData, curNode);
    });
    parentMenu.add(abstractAction).setMnemonic(KeyEvent.VK_E);
  }

  private void addExportStatsFull(final JMenu parentMenu) {
    final Action showDiceStats = SwingAction.of("Export Full Game Stats", e -> createAndSaveStats(true));
    parentMenu.add(showDiceStats).setMnemonic(KeyEvent.VK_F);
  }

  private void addExportStats(final JMenu parentMenu) {
    final Action showDiceStats = SwingAction.of("Export Short Game Stats", e -> createAndSaveStats(false));
    parentMenu.add(showDiceStats).setMnemonic(KeyEvent.VK_S);
  }

  private void createAndSaveStats(final boolean showPhaseStats) {
    final ExtendedStats statPanel = new ExtendedStats(gameData, uiContext);
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    final File rootDir = new File(SystemProperties.getUserDir());
    final int currentRound = gameData.getCurrentRound();
    String defaultFileName =
        "stats_" + dateTimeFormatter.format(LocalDateTime.now()) + "_" + gameData.getGameName() + "_round_"
            + currentRound + (showPhaseStats ? "_full" : "_short");
    defaultFileName = FileNameUtils.removeIllegalCharacters(defaultFileName);
    defaultFileName = defaultFileName + ".csv";
    chooser.setSelectedFile(new File(rootDir, defaultFileName));
    if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
      return;
    }
    final StringBuilder text = new StringBuilder(1000);
    final GameData clone;
    try {
      gameData.acquireReadLock();
      clone = GameDataUtils.cloneGameData(gameData);
      final IStat[] stats = statPanel.getStats();
      // extended stats covers stuff that doesn't show up in the game stats menu bar, like custom resources or tech
      // tokens or # techs, etc.
      final IStat[] statsExtended = statPanel.getStatsExtended(gameData);
      final String[] alliances = statPanel.getAlliances().toArray(new String[statPanel.getAlliances().size()]);
      final PlayerID[] players = statPanel.getPlayers().toArray(new PlayerID[statPanel.getPlayers().size()]);
      // its important here to translate the player objects into our game data
      // the players for the stat panel are only relevant with respect to
      // the game data they belong to
      for (int i = 0; i < players.length; i++) {
        players[i] = clone.getPlayerList().getPlayerId(players[i].getName());
      }
      text.append(defaultFileName + ",");
      text.append("\n");
      text.append("TripleA Engine Version: ,");
      text.append(ClientContext.engineVersion() + ",");
      text.append("\n");
      text.append("Game Name: ,");
      text.append(gameData.getGameName() + ",");
      text.append("\n");
      text.append("Game Version: ,");
      text.append(gameData.getGameVersion() + ",");
      text.append("\n");
      text.append("\n");
      text.append("Current Round: ,");
      text.append(currentRound + ",");
      text.append("\n");
      text.append("Number of Players: ,");
      text.append(statPanel.getPlayers().size() + ",");
      text.append("\n");
      text.append("Number of Alliances: ,");
      text.append(statPanel.getAlliances().size() + ",");
      text.append("\n");
      text.append("\n");
      text.append("Turn Order: ,");
      text.append("\n");
      final List<PlayerID> playerOrderList = new ArrayList<>();
      playerOrderList.addAll(gameData.getPlayerList().getPlayers());
      Collections.sort(playerOrderList, new PlayerOrderComparator(gameData));
      final Set<PlayerID> playerOrderSetNoDuplicates = new LinkedHashSet<>(playerOrderList);
      for (final PlayerID currentPlayerId : playerOrderSetNoDuplicates) {
        text.append(currentPlayerId.getName()).append(",");
        final Collection<String> allianceNames = gameData.getAllianceTracker().getAlliancesPlayerIsIn(currentPlayerId);
        for (final String allianceName : allianceNames) {
          text.append(allianceName).append(",");
        }
        text.append("\n");
      }
      text.append("\n");
      text.append("Winners: ,");
      final EndRoundDelegate delegateEndRound = (EndRoundDelegate) gameData.getDelegateList().getDelegate("endRound");
      if (delegateEndRound != null && delegateEndRound.getWinners() != null) {
        for (final PlayerID p : delegateEndRound.getWinners()) {
          text.append(p.getName()).append(",");
        }
      } else {
        text.append("none yet; game not over,");
      }
      text.append("\n");
      text.append("\n");
      text.append("Resource Chart: ,");
      text.append("\n");
      for (final Resource resource : gameData.getResourceList().getResources()) {
        text.append(resource.getName() + ",");
        text.append("\n");
      }
      // if short, we won't both showing production and unit info
      if (showPhaseStats) {
        text.append("\n");
        text.append("Production Rules: ,");
        text.append("\n");
        text.append("Name,Result,Quantity,Cost,Resource,\n");
        final Collection<ProductionRule> purchaseOptions = gameData.getProductionRuleList().getProductionRules();
        for (final ProductionRule pr : purchaseOptions) {
          String costString = pr.toStringCosts().replaceAll("; ", ",");
          costString = costString.replaceAll(" ", ",");
          text.append(pr.getName()).append(",").append(pr.getResults().keySet().iterator().next().getName()).append(",")
              .append(pr.getResults().getInt(pr.getResults().keySet().iterator().next())).append(",").append(costString)
              .append(",");
          text.append("\n");
        }
        text.append("\n");
        text.append("Unit Types: ,");
        text.append("\n");
        text.append("Name,Listed Abilities\n");
        for (final UnitType unitType : gameData.getUnitTypeList()) {
          final UnitAttachment ua = UnitAttachment.get(unitType);
          if (ua == null) {
            continue;
          }
          String toModify = ua.allUnitStatsForExporter();
          toModify = toModify.replaceFirst("UnitType called ", "").replaceFirst(" with:", "")
              .replaceAll("games.strategy.engine.data.", "").replaceAll("\n", ";").replaceAll(",", ";");
          toModify = toModify.replaceAll("  ", ",");
          toModify = toModify.replaceAll(", ", ",").replaceAll(" ,", ",");
          text.append(toModify);
          text.append("\n");
        }
      }
      text.append("\n");
      text.append((showPhaseStats ? "Full Stats (includes each phase that had activity),"
          : "Short Stats (only shows first phase with activity per player per round),"));
      text.append("\n");
      text.append("Turn Stats: ,");
      text.append("\n");
      text.append("Round,Player Turn,Phase Name,");
      for (final IStat stat : stats) {
        for (final PlayerID player : players) {
          text.append(stat.getName()).append(" ");
          text.append(player.getName());
          text.append(",");
        }
        for (final String alliance : alliances) {
          text.append(stat.getName()).append(" ");
          text.append(alliance);
          text.append(",");
        }
      }
      for (final IStat element : statsExtended) {
        for (final PlayerID player : players) {
          text.append(element.getName()).append(" ");
          text.append(player.getName());
          text.append(",");
        }
        for (final String alliance : alliances) {
          text.append(element.getName()).append(" ");
          text.append(alliance);
          text.append(",");
        }
      }
      text.append("\n");
      clone.getHistory().gotoNode(clone.getHistory().getLastNode());
      @SuppressWarnings("unchecked")
      final Enumeration<TreeNode> nodes = ((DefaultMutableTreeNode) clone.getHistory().getRoot()).preorderEnumeration();
      PlayerID currentPlayer = null;
      int round = 0;
      while (nodes.hasMoreElements()) {
        // we want to export on change of turn
        final HistoryNode element = (HistoryNode) nodes.nextElement();
        if (element instanceof Round) {
          round++;
        }
        if (!(element instanceof Step)) {
          continue;
        }
        final Step step = (Step) element;
        if (step.getPlayerId() == null || step.getPlayerId().isNull()) {
          continue;
        }
        // this is to stop from having multiple entries for each players turn.
        if (!showPhaseStats) {
          if (step.getPlayerId() == currentPlayer) {
            continue;
          }
        }
        currentPlayer = step.getPlayerId();
        clone.getHistory().gotoNode(element);
        final String playerName = step.getPlayerId() == null ? "" : step.getPlayerId().getName() + ": ";
        String stepName = step.getStepName();
        // copied directly from TripleAPlayer, will probably have to be updated in the future if more delegates are made
        if (stepName.endsWith("Bid")) {
          stepName = "Bid";
        } else if (stepName.endsWith("Tech")) {
          stepName = "Tech";
        } else if (stepName.endsWith("TechActivation")) {
          stepName = "TechActivation";
        } else if (stepName.endsWith("Purchase")) {
          stepName = "Purchase";
        } else if (stepName.endsWith("NonCombatMove")) {
          stepName = "NonCombatMove";
        } else if (stepName.endsWith("Move")) {
          stepName = "Move";
        } else if (stepName.endsWith("Battle")) {
          stepName = "Battle";
        } else if (stepName.endsWith("BidPlace")) {
          stepName = "BidPlace";
        } else if (stepName.endsWith("Place")) {
          stepName = "Place";
        } else if (stepName.endsWith("Politics")) {
          stepName = "Politics";
        } else if (stepName.endsWith("EndTurn")) {
          stepName = "EndTurn";
        } else {
          stepName = "";
        }
        text.append(round).append(",").append(playerName).append(",").append(stepName).append(",");
        for (final IStat stat : stats) {
          for (final PlayerID player : players) {
            text.append(stat.getFormatter().format(stat.getValue(player, clone)));
            text.append(",");
          }
          for (final String alliance : alliances) {
            text.append(stat.getFormatter().format(stat.getValue(alliance, clone)));
            text.append(",");
          }
        }
        for (final IStat element2 : statsExtended) {
          for (final PlayerID player : players) {
            text.append(element2.getFormatter().format(element2.getValue(player, clone)));
            text.append(",");
          }
          for (final String alliance : alliances) {
            text.append(element2.getFormatter().format(element2.getValue(alliance, clone)));
            text.append(",");
          }
        }
        text.append("\n");
      }
    } finally {
      gameData.releaseReadLock();
    }
    try (Writer writer = new FileWriter(chooser.getSelectedFile())) {
      writer.write(text.toString());
    } catch (final IOException e1) {
      ClientLogger.logQuietly("Failed to write stats: " + chooser.getSelectedFile().getAbsolutePath(), e1);
    }
  }

  private void addExportUnitStats(final JMenu parentMenu) {
    final JMenuItem menuFileExport = new JMenuItem(SwingAction.of("Export Unit Charts", e -> {
      final JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      final File rootDir = new File(SystemProperties.getUserDir());
      String defaultFileName = gameData.getGameName() + "_unit_stats";
      defaultFileName = FileNameUtils.removeIllegalCharacters(defaultFileName);
      defaultFileName = defaultFileName + ".html";
      chooser.setSelectedFile(new File(rootDir, defaultFileName));
      if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
        return;
      }
      try (Writer writer = new FileWriter(chooser.getSelectedFile())) {
        writer.write(
            HelpMenu.getUnitStatsTable(gameData, uiContext).replaceAll("<p>", "<p>\r\n").replaceAll("</p>", "</p>\r\n")
                .replaceAll("</tr>", "</tr>\r\n").replaceAll(LocalizeHtml.PATTERN_HTML_IMG_TAG, ""));
      } catch (final IOException e1) {
        ClientLogger.logQuietly("Failed to write unit stats: " + chooser.getSelectedFile().getAbsolutePath(), e1);
      }

    }));
    menuFileExport.setMnemonic(KeyEvent.VK_U);
    parentMenu.add(menuFileExport);
  }


  private void addExportSetupCharts(final JMenu parentMenu) {
    final JMenuItem menuFileExport = new JMenuItem(SwingAction.of("Export Setup Charts", e -> {
      final JFrame frame = new JFrame("Export Setup Charts");
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      GameData clonedGameData;
      gameData.acquireReadLock();
      try {
        clonedGameData = GameDataUtils.cloneGameData(gameData);
      } finally {
        gameData.releaseReadLock();
      }
      final JComponent newContentPane = new SetupFrame(clonedGameData);
      // content panes must be opaque
      newContentPane.setOpaque(true);
      frame.setContentPane(newContentPane);
      // Display the window.
      frame.pack();
      frame.setLocationRelativeTo(frame);
      frame.setVisible(true);
      uiContext.addShutdownWindow(frame);

    }));
    menuFileExport.setMnemonic(KeyEvent.VK_C);
    parentMenu.add(menuFileExport);

  }
}
