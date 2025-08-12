package games.strategy.triplea.printgenerator;

import com.google.common.collect.Iterables;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.ui.ExtendedStats;
import games.strategy.triplea.ui.UiContext;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import org.triplea.config.product.ProductVersionReader;

public class StatsInfo extends InfoForFile {
  private final UiContext uiContext;
  final boolean showPhaseStats;
  GameData gameData;
  List<GamePlayer> orderedPlayers;
  Set<String> alliances;
  Iterable<IStat> stats;

  public StatsInfo(Path chosenOutFile, UiContext uiContext, boolean showPhaseStats) {
    super(chosenOutFile);
    this.uiContext = uiContext;
    this.showPhaseStats = showPhaseStats;
  }

  @Override
  protected void gatherDataBeforeWriting(PrintGenerationData printData) {
    gameData = printData.getData();
    alliances = gameData.getAllianceTracker().getAlliances();
    orderedPlayers = gameData.getPlayerList().getSortedPlayers();
    // extended stats covers stuff that doesn't show up in the game stats menu bar, like custom
    // resources or tech  tokens or # techs, etc.
    final ExtendedStats statPanel = new ExtendedStats(gameData, uiContext);
    stats =
        Iterables.concat(
            List.of(statPanel.getStats()), List.of(statPanel.getStatsExtended(gameData)));
  }

  @Override
  protected void writeIntoFile(Writer writer) throws IOException {
    writeHeaderInfo(writer);
    writer.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    writer.append("Resource Chart:").append(DELIMITER);
    for (final Resource resource : gameData.getResourceList().getResources()) {
      writer.append(resource.getName()).append(DELIMITER).append(LINE_SEPARATOR);
    }
    // if short, we won't both showing production and unit info
    if (showPhaseStats) {
      writeProductionRulesAndUnitInfo(writer);
    }
    writer.append(LINE_SEPARATOR);
    writer
        .append(
            showPhaseStats
                ? "Full Stats (includes each phase that had activity),"
                : "Short Stats (only shows first phase with activity per player per round),")
        .append(LINE_SEPARATOR);
    writer.append("Turn Stats:").append(DELIMITER);
    writer.append("Round,Player Turn,Phase Name,");
    // its important here to use the player objects from the cloned game data
    // the players for the stat panel are only relevant with respect to the game data they belong
    // to
    for (final IStat stat : stats) {
      for (final GamePlayer player : orderedPlayers) {
        writer.append(stat.getName()).append(' ').append(player.getName()).append(DELIMITER);
      }
      for (final String alliance : alliances) {
        writer.append(stat.getName()).append(' ').append(alliance).append(DELIMITER);
      }
    }
    writer.append(LINE_SEPARATOR);
    gameData.getHistory().gotoNode(gameData.getHistory().getLastNode());
    final Enumeration<TreeNode> nodes =
        ((DefaultMutableTreeNode) gameData.getHistory().getRoot()).preorderEnumeration();
    writeFromHistoryNodes(writer, nodes);
  }

  private void writeHeaderInfo(Writer writer) throws IOException {
    writer
        .append(String.format("stats_%s", showPhaseStats ? "full" : "short"))
        .append(DELIMITER)
        .append(LINE_SEPARATOR)
        .append("TripleA Engine Version:")
        .append(DELIMITER)
        .append(ProductVersionReader.getCurrentVersion().toString())
        .append(DELIMITER)
        .append(LINE_SEPARATOR)
        .append("Game Name:")
        .append(DELIMITER)
        .append(gameData.getGameName())
        .append(DELIMITER)
        .append(LINE_SEPARATOR)
        .append(LINE_SEPARATOR)
        .append("Current Round:")
        .append(DELIMITER);
    final int currentRound = gameData.getCurrentRound();
    writer.write(currentRound);
    writer.append(DELIMITER).append(LINE_SEPARATOR).append("Number of Players:").append(DELIMITER);
    writer.write(gameData.getPlayerList().size());
    writer
        .append(DELIMITER)
        .append(LINE_SEPARATOR)
        .append("Number of Alliances:")
        .append(DELIMITER);
    writer.write(alliances.size());
    writer
        .append(DELIMITER)
        .append(LINE_SEPARATOR)
        .append(LINE_SEPARATOR)
        .append("Turn Order:")
        .append(DELIMITER);
    for (final GamePlayer currentGamePlayer : orderedPlayers) {
      writer.append(currentGamePlayer.getName()).append(DELIMITER);
      final Collection<String> allianceNames =
          gameData.getAllianceTracker().getAlliancesPlayerIsIn(currentGamePlayer);
      for (final String allianceName : allianceNames) {
        writer.append(allianceName).append(DELIMITER);
      }
      writer.append(LINE_SEPARATOR);
    }
    writer.append(LINE_SEPARATOR);
    writer.append("Winners:").append(DELIMITER);
    final EndRoundDelegate delegateEndRound = (EndRoundDelegate) gameData.getDelegate("endRound");
    if (delegateEndRound != null && delegateEndRound.getWinners() != null) {
      for (final GamePlayer p : delegateEndRound.getWinners()) {
        writer.append(p.getName()).append(DELIMITER);
      }
    } else {
      writer.append("none yet; game not over,");
    }
  }

  private void writeFromHistoryNodes(Writer writer, Enumeration<TreeNode> nodes)
      throws IOException {
    Optional<GamePlayer> optionalCurrentPlayer = Optional.empty();
    int round = 0;
    while (nodes.hasMoreElements()) {
      // we want to export on change of turn
      final HistoryNode element = (HistoryNode) nodes.nextElement();
      if (element instanceof Round) {
        round++;
      }
      final Step step =
          getValidStepFromHistoryNode(element, optionalCurrentPlayer.orElse(null)).orElse(null);
      if (step == null) {
        continue;
      }
      optionalCurrentPlayer = step.getPlayerId();
      final String playerName =
          optionalCurrentPlayer.map(gamePlayer -> gamePlayer.getName() + ": ").orElse("");
      gameData.getHistory().gotoNode(element);
      writeRoundStepStats(writer, round, playerName, step);
      writer.append(LINE_SEPARATOR);
    }
  }

  private Optional<Step> getValidStepFromHistoryNode(
      final HistoryNode element, final GamePlayer currentPlayer) {
    if (!(element instanceof Step step)) {
      return Optional.empty();
    }
    final Optional<GamePlayer> optionalStepPlayer = step.getPlayerId();
    if (optionalStepPlayer.isEmpty() || optionalStepPlayer.get().isNull()) {
      return Optional.empty();
    }
    // this is to stop from having multiple entries for each players turn.
    if (!showPhaseStats && optionalStepPlayer.get().equals(currentPlayer)) {
      return Optional.empty();
    }
    return Optional.of(step);
  }

  private void writeRoundStepStats(Writer writer, int round, String playerName, Step step)
      throws IOException {
    writer.write(round);
    writer
        .append(DELIMITER)
        .append(playerName)
        .append(DELIMITER)
        .append(getStepNameToBeWritten(step))
        .append(DELIMITER);
    for (final IStat stat : stats) {
      for (final GamePlayer player : orderedPlayers) {
        writer
            .append(
                IStat.DECIMAL_FORMAT.format(
                    stat.getValue(player, gameData, uiContext.getMapData())))
            .append(DELIMITER);
      }
      for (final String alliance : alliances) {
        writer
            .append(
                IStat.DECIMAL_FORMAT.format(
                    stat.getValue(alliance, gameData, uiContext.getMapData())))
            .append(DELIMITER);
      }
    }
  }

  private static String getStepNameToBeWritten(Step step) {
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
    return stepName;
  }

  private void writeProductionRulesAndUnitInfo(Writer writer) throws IOException {
    writer.append(LINE_SEPARATOR);
    writer.append("Production Rules:").append(DELIMITER);
    writer.append("Name,Result,Quantity,Cost,Resource,\n");
    final Collection<ProductionRule> purchaseOptions =
        gameData.getProductionRuleList().getProductionRules();
    for (final ProductionRule pr : purchaseOptions) {
      final String costString = pr.toStringCosts().replaceAll(";? ", ",");
      writer.append(pr.getName()).append(DELIMITER);
      writer.append(pr.getAnyResultKey().getName()).append(DELIMITER);
      writer.write(pr.getResults().getInt(pr.getAnyResultKey()));
      writer.append(DELIMITER).append(costString).append(DELIMITER).append(LINE_SEPARATOR);
    }
    writer.append(LINE_SEPARATOR);
    writer.append("Unit Types:").append(DELIMITER);
    writer.append("Name,Listed Abilities\n");
    for (final UnitType unitType : gameData.getUnitTypeList()) {
      final UnitAttachment ua = unitType.getUnitAttachment();
      if (ua == null) {
        continue;
      }
      final String toModify =
          ua.allUnitStatsForExporter()
              .replaceAll("UnitType called | with:|games\\.strategy\\.engine\\.data\\.", "")
              .replaceAll("[\n,]", ";")
              .replaceAll(" {2}| ?, ?", ",");
      writer.append(toModify);
    }
  }

  public static void export(
      Path chosenOutFile, UiContext uiContext, boolean showPhaseStats, GameData clonedGameData) {
    final StatsInfo statsInfo = new StatsInfo(chosenOutFile, uiContext, showPhaseStats);
    final PrintGenerationData printData =
        PrintGenerationData.builder().outDir(null).data(clonedGameData).build();
    statsInfo.gatherDataBeforeWriting(printData);
    statsInfo.saveToFile(printData);
  }
}
