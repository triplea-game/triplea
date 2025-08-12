package games.strategy.triplea.ui.menubar;

import com.google.common.collect.Iterables;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.export.GameDataExporter;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.history.History;
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
import games.strategy.triplea.ui.menubar.help.UnitStatsTable;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.map.data.elements.Game;
import org.triplea.map.xml.writer.GameXmlWriter;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.util.FileNameUtils;

@Slf4j
final class ExportMenu extends JMenu {
  private static final long serialVersionUID = 8416990293444575737L;

  private final TripleAFrame frame;
  private final GameData gameData;
  private final UiContext uiContext;
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");

  ExportMenu(final TripleAFrame frame) {
    super("Export");

    this.frame = frame;
    gameData = frame.getGame().getData();
    uiContext = frame.getUiContext();

    setMnemonic(KeyEvent.VK_E);

    add(createExportXmlMenu());
    add(createExportStatsMenu());
    add(createExportStatsFullMenu());
    add(createExportSetupChartsMenu());
    add(createExportUnitStatsMenu());
    add(createSaveScreenshotMenu());
  }

  // TODO: create a second menu option for parsing current attachments
  private JMenuItem createExportXmlMenu() {
    return new JMenuItemBuilder("Export game.xml File (Beta)", KeyCode.X)
        .actionListener(this::exportXmlFile)
        .build();
  }

  private void exportXmlFile() {
    // The existing XML file is needed for attachment ordering data.
    final Path gameXmlPath = gameData.getGameXmlPath(uiContext.getMapLocation()).orElse(null);
    if (gameXmlPath == null) {
      JOptionPane.showMessageDialog(frame, "Error: Existing XML file not found.");
      return;
    }

    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    final int round = gameData.getCurrentRound();
    final String defaultFileName =
        FileNameUtils.removeIllegalCharacters(
                String.format(
                    "xml_%s_%s_round_%s",
                    dateTimeFormatter.format(LocalDateTime.now(ZoneId.systemDefault())),
                    gameData.getGameName(),
                    round))
            + ".xml";
    final Path rootDir = ClientFileSystemHelper.getUserRootFolder();
    chooser.setSelectedFile(rootDir.resolve(defaultFileName).toFile());
    if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
      return;
    }
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      final Game xmlGameModel = GameDataExporter.convertToXmlModel(gameData, gameXmlPath);
      GameXmlWriter.exportXml(xmlGameModel, chooser.getSelectedFile().toPath());
    }
  }

  private JMenuItem createSaveScreenshotMenu() {
    return new JMenuItemBuilder("Export Gameboard Picture", KeyCode.E)
        .actionListener(this::saveScreenshot)
        .build();
  }

  private void saveScreenshot() {
    // get current history node. if we are in history view, get the selected node.
    final HistoryPanel historyPanel = frame.getHistoryPanel();
    final HistoryNode curNode;
    if (historyPanel == null) {
      curNode = gameData.getHistory().getLastNode();
    } else {
      curNode = historyPanel.getCurrentNode();
    }
    ScreenshotExporter.exportScreenshot(frame, gameData, curNode);
  }

  private JMenuItem createExportStatsFullMenu() {
    return new JMenuItemBuilder("Export Full Game Stats", KeyCode.F)
        .actionListener(() -> createAndSaveStats(true))
        .build();
  }

  private JMenuItem createExportStatsMenu() {
    return new JMenuItemBuilder("Export Short Game Stats", KeyCode.S)
        .actionListener(() -> createAndSaveStats(false))
        .build();
  }

  private void createAndSaveStats(final boolean showPhaseStats) {
    final ExtendedStats statPanel = new ExtendedStats(gameData, uiContext);
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    final Path rootDir = Path.of(SystemProperties.getUserDir());
    final int currentRound = gameData.getCurrentRound();
    final String defaultFileName =
        FileNameUtils.removeIllegalCharacters(
                String.format(
                    "stats_%s_%s_round_%s_%s",
                    dateTimeFormatter.format(LocalDateTime.now(ZoneId.systemDefault())),
                    gameData.getGameName(),
                    currentRound,
                    showPhaseStats ? "full" : "short"))
            + ".csv";
    chooser.setSelectedFile(rootDir.resolve(defaultFileName).toFile());
    final CompletableFuture<GameData> cloneCompletableFuture = getGameDataCloneWithHistory();
    if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
      return;
    }
    final GameData clone = cloneCompletableFuture.join();
    try (PrintWriter writer = new PrintWriter(chooser.getSelectedFile(), StandardCharsets.UTF_8);
        GameData.Unlocker ignored = clone.acquireReadLock()) {
      writer.append(defaultFileName).println(',');
      writer.append("TripleA Engine Version: ,");
      writer.append(ProductVersionReader.getCurrentVersion().toString()).println(',');
      writer.append("Game Name: ,");
      writer.append(clone.getGameName()).println(',');
      writer.println();
      writer.append("Current Round: ,");
      writer.print(currentRound);
      writer.println(',');
      writer.append("Number of Players: ,");
      writer.print(clone.getPlayerList().size());
      writer.println(',');
      writer.append("Number of Alliances: ,");
      writer.print(clone.getAllianceTracker().getAlliances().size());
      writer.println(',');
      writer.println();
      writer.println("Turn Order: ,");
      final List<GamePlayer> orderedPlayers = clone.getPlayerList().getSortedPlayers();
      for (final GamePlayer currentGamePlayer : orderedPlayers) {
        writer.append(currentGamePlayer.getName()).append(',');
        final Collection<String> allianceNames =
            clone.getAllianceTracker().getAlliancesPlayerIsIn(currentGamePlayer);
        for (final String allianceName : allianceNames) {
          writer.append(allianceName).append(',');
        }
        writer.println();
      }
      writer.println();
      writer.append("Winners: ,");
      final EndRoundDelegate delegateEndRound = (EndRoundDelegate) clone.getDelegate("endRound");
      if (delegateEndRound != null && delegateEndRound.getWinners() != null) {
        for (final GamePlayer p : delegateEndRound.getWinners()) {
          writer.append(p.getName()).append(',');
        }
      } else {
        writer.append("none yet; game not over,");
      }
      writer.println();
      writer.println();
      writer.println("Resource Chart: ,");
      for (final Resource resource : clone.getResourceList().getResources()) {
        writer.append(resource.getName()).println(',');
      }
      // if short, we won't both showing production and unit info
      if (showPhaseStats) {
        writer.println();
        writer.println("Production Rules: ,");
        writer.append("Name,Result,Quantity,Cost,Resource,\n");
        final Collection<ProductionRule> purchaseOptions =
            clone.getProductionRuleList().getProductionRules();
        for (final ProductionRule pr : purchaseOptions) {
          final String costString = pr.toStringCosts().replaceAll(";? ", ",");
          writer.append(pr.getName()).append(',');
          writer.append(pr.getAnyResultKey().getName()).append(',');
          writer.print(pr.getResults().getInt(pr.getAnyResultKey()));
          writer.append(',').append(costString).println(',');
        }
        writer.println();
        writer.println("Unit Types: ,");
        writer.append("Name,Listed Abilities\n");
        for (final UnitType unitType : clone.getUnitTypeList()) {
          final UnitAttachment ua = unitType.getUnitAttachment();
          if (ua == null) {
            continue;
          }
          final String toModify =
              ua.allUnitStatsForExporter()
                  .replaceAll("UnitType called | with:|games\\.strategy\\.engine\\.data\\.", "")
                  .replaceAll("[\n,]", ";")
                  .replaceAll(" {2}| ?, ?", ",");
          writer.println(toModify);
        }
      }
      writer.println();
      writer.println(
          showPhaseStats
              ? "Full Stats (includes each phase that had activity),"
              : "Short Stats (only shows first phase with activity per player per round),");
      writer.println("Turn Stats: ,");
      writer.append("Round,Player Turn,Phase Name,");
      final Set<String> alliances = clone.getAllianceTracker().getAlliances();
      // its important here to use the player objects from the cloned game data
      // the players for the stat panel are only relevant with respect to the game data they belong
      // to
      final List<GamePlayer> players = clone.getPlayerList().getSortedPlayers();

      // extended stats covers stuff that doesn't show up in the game stats menu bar, like custom
      // resources or tech  tokens or # techs, etc.
      final Iterable<IStat> stats =
          Iterables.concat(
              List.of(statPanel.getStats()), List.of(statPanel.getStatsExtended(clone)));
      for (final IStat stat : stats) {
        for (final GamePlayer player : players) {
          writer.append(stat.getName()).append(' ');
          writer.append(player.getName()).append(',');
        }
        for (final String alliance : alliances) {
          writer.append(stat.getName()).append(' ');
          writer.append(alliance).append(',');
        }
      }
      writer.println();
      clone.getHistory().gotoNode(clone.getHistory().getLastNode());
      final Enumeration<TreeNode> nodes =
          ((DefaultMutableTreeNode) clone.getHistory().getRoot()).preorderEnumeration();
      Optional<GamePlayer> optionalCurrentPlayer = Optional.empty();
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
        final Optional<GamePlayer> optionalStepPlayer = step.getPlayerId();
        if (optionalStepPlayer.isEmpty() || optionalStepPlayer.get().isNull()) {
          continue;
        }
        // this is to stop from having multiple entries for each players turn.
        if (!showPhaseStats
            && optionalStepPlayer.get().equals(optionalCurrentPlayer.orElse(null))) {
          continue;
        }
        optionalCurrentPlayer = step.getPlayerId();
        clone.getHistory().gotoNode(element);
        final String playerName =
            optionalCurrentPlayer.isEmpty() ? "" : optionalCurrentPlayer.get().getName() + ": ";
        String stepName = step.getStepName();
        // copied directly from TripleAPlayer, will probably have to be updated in the future if
        // more delegates are made
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
        writer.print(round);
        writer.append(',').append(playerName).append(',').append(stepName).append(',');
        for (final IStat stat : stats) {
          for (final GamePlayer player : players) {
            writer
                .append(
                    IStat.DECIMAL_FORMAT.format(
                        stat.getValue(player, clone, uiContext.getMapData())))
                .append(',');
          }
          for (final String alliance : alliances) {
            writer
                .append(
                    IStat.DECIMAL_FORMAT.format(
                        stat.getValue(alliance, clone, uiContext.getMapData())))
                .append(',');
          }
        }
        writer.println();
      }
    } catch (final IOException e) {
      log.error("Failed to write stats: " + chooser.getSelectedFile().getAbsolutePath(), e);
    }
  }

  private JMenuItem createExportUnitStatsMenu() {
    return new JMenuItemBuilder("Export Unit Charts", KeyCode.U)
        .actionListener(this::exportUnitCharts)
        .build();
  }

  private void exportUnitCharts() {
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    final Path rootDir = Path.of(SystemProperties.getUserDir());
    final String defaultFileName =
        FileNameUtils.removeIllegalCharacters(gameData.getGameName()) + "_unit_stats.html";
    chooser.setSelectedFile(rootDir.resolve(defaultFileName).toFile());
    if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
      return;
    }
    try (Writer writer =
        Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
      writer.write(
          UnitStatsTable.getUnitStatsTable(gameData, uiContext)
              .replaceAll("</?p>|</tr>", "$0\r\n")
              .replaceAll("(?i)<img[^>]+/>", ""));
    } catch (final IOException e1) {
      log.error("Failed to write unit stats: " + chooser.getSelectedFile().getAbsolutePath(), e1);
    }
  }

  private JMenuItem createExportSetupChartsMenu() {
    return new JMenuItemBuilder("Export Setup Charts", KeyCode.C)
        .actionListener(this::exportSetupCharts)
        .build();
  }

  private void exportSetupCharts() {
    final JFrame frameExportSetupCharte = new JFrame("Export Setup Charts");
    frameExportSetupCharte.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    final JComponent newContentPane = new SetupFrame(getGameDataCloneWithHistory());
    // content panes must be opaque
    newContentPane.setOpaque(true);
    frameExportSetupCharte.setContentPane(newContentPane);
    // Display the window.
    frameExportSetupCharte.pack();
    frameExportSetupCharte.setLocationRelativeTo(frameExportSetupCharte);
    frameExportSetupCharte.setVisible(true);
    uiContext.addShutdownWindow(frameExportSetupCharte);
  }

  /**
   * Provides a game data copy with its history by triggering an asynchronous process for the
   * copying task.
   *
   * @return {@link GameData} clone as {@link CompletableFuture} incl. {@link History} which has
   *     already {@link History#enableSeeking(HistoryPanel)} called. Fails with {@link
   *     IllegalStateException} in case the cloning fails.
   */
  private CompletableFuture<GameData> getGameDataCloneWithHistory() {
    return CompletableFuture.supplyAsync(
        () ->
            GameDataUtils.cloneGameDataWithHistory(gameData, true)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "ExportMenu: Cloning of game data for exporting failed.")));
  }
}
