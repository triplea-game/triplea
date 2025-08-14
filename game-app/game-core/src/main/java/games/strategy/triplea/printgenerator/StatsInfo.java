package games.strategy.triplea.printgenerator;

import com.google.common.collect.Iterables;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
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
  int totalColumns;
  private String emptyColumnsExceptOne;
  private String emptyColumnsExceptTwo;

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
    final List<IStat> statPanelStats = List.of(statPanel.getStats());
    final List<IStat> statPanelStatsExtended = List.of(statPanel.getStatsExtended(gameData));
    stats = Iterables.concat(statPanelStats, statPanelStatsExtended);
    // CSV files need to on each row the same number of delimiters, so calculate total number coming
    // from the round steps
    totalColumns =
        2
            + (statPanelStats.size() + statPanelStatsExtended.size())
                * (orderedPlayers.size() + alliances.size());
    emptyColumnsExceptOne = DELIMITER.repeat(totalColumns - 1);
    emptyColumnsExceptTwo = DELIMITER.repeat(totalColumns - 2);
  }

  @Override
  protected void writeIntoFile(Writer writer) throws IOException {
    writeHeaderInfo(writer);
    writer.append(LINE_SEPARATOR);

    writer.append("Resource Chart:").append(DELIMITER);
    final Collection<Resource> resources = gameData.getResourceList().getResources();
    for (final Resource resource : resources) {
      writer.append(resource.getName()).append(DELIMITER);
    }
    writer.append(DELIMITER.repeat(totalColumns - 1 - resources.size()));
    writer.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    // if short, we won't both showing production and unit info
    if (showPhaseStats) {
      writeProductionRulesAndUnitInfo(writer);
    }
    writeStatsHeader(writer);
    writeFromHistoryNodes(writer);
  }

  private void writeStatsHeader(Writer writer) throws IOException {
    writer
        .append(
            showPhaseStats
                ? "Full Stats (includes each phase that had activity)"
                : "Short Stats (only shows first phase with activity per player per round)")
        .append(emptyColumnsExceptOne)
        .append(LINE_SEPARATOR);
    writer.append("Turn Stats:").append(emptyColumnsExceptOne).append(LINE_SEPARATOR);
    writer
        .append("Round")
        .append(DELIMITER)
        .append("Player Turn")
        .append(DELIMITER)
        .append("Phase Name")
        .append(DELIMITER);
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
  }

  private void writeHeaderInfo(Writer writer) throws IOException {
    writer
        .append(String.format("stats_%s", showPhaseStats ? "full" : "short"))
        .append(emptyColumnsExceptOne)
        .append(LINE_SEPARATOR);
    writer
        .append("TripleA Engine Version:")
        .append(DELIMITER)
        .append(ProductVersionReader.getCurrentVersion().toString())
        .append(emptyColumnsExceptTwo)
        .append(LINE_SEPARATOR);
    writer
        .append("Game Name:")
        .append(DELIMITER)
        .append(gameData.getGameName())
        .append(emptyColumnsExceptTwo)
        .append(LINE_SEPARATOR)
        .append(LINE_SEPARATOR);

    writer
        .append("Current Round:")
        .append(DELIMITER)
        .append(Integer.toString(gameData.getCurrentRound()))
        .append(emptyColumnsExceptTwo)
        .append(LINE_SEPARATOR);

    writer
        .append("Number of Players:")
        .append(DELIMITER)
        .append(Integer.toString(orderedPlayers.size()))
        .append(emptyColumnsExceptTwo)
        .append(LINE_SEPARATOR);

    writer.append("Number of Alliances:").append(DELIMITER);
    writer.append(Integer.toString(alliances.size()));
    writer.append(emptyColumnsExceptTwo).append(LINE_SEPARATOR).append(LINE_SEPARATOR);

    writeTurnOrder(writer);

    writeWinners(writer);
  }

  private void writeWinners(Writer writer) throws IOException {
    writer.append("Winners:").append(DELIMITER);
    final EndRoundDelegate delegateEndRound = (EndRoundDelegate) gameData.getDelegate("endRound");
    if (delegateEndRound != null && !delegateEndRound.isGameOver()) {
      Collection<GamePlayer> winners = delegateEndRound.getWinners();
      for (final GamePlayer p : winners) {
        writer.append(p.getName()).append(DELIMITER);
      }
      writer.append(DELIMITER.repeat(totalColumns - 1 - winners.size()));
    } else {
      writer.append("none yet; game not over").append(emptyColumnsExceptTwo);
    }
    writer.append(LINE_SEPARATOR);
  }

  private void writeTurnOrder(Writer writer) throws IOException {
    writer.append("Turn Order:").append(emptyColumnsExceptOne).append(LINE_SEPARATOR);
    for (final GamePlayer currentGamePlayer : orderedPlayers) {
      writer.append(currentGamePlayer.getName()).append(DELIMITER);
      final Collection<String> allianceNames =
          gameData.getAllianceTracker().getAlliancesPlayerIsIn(currentGamePlayer);
      writer.append(String.join(";", allianceNames));
      writer.append(emptyColumnsExceptTwo).append(LINE_SEPARATOR);
    }
    writer.append(LINE_SEPARATOR);
  }

  private void writeFromHistoryNodes(Writer writer) throws IOException {
    gameData.getHistory().gotoNode(gameData.getHistory().getLastNode());
    final Enumeration<TreeNode> nodes =
        ((DefaultMutableTreeNode) gameData.getHistory().getRoot()).preorderEnumeration();
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
    writer.append(Integer.toString(round));
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
    if (GameStep.isBidStepName(stepName)) {
      stepName = "Bid";
    } else if (GameStep.isTechStepName(stepName)) {
      stepName = "Tech";
    } else if (GameStep.isTechActivationStepName(stepName)) {
      stepName = "TechActivation";
    } else if (GameStep.isPurchaseStepName(stepName)) {
      stepName = "Purchase";
    } else if (GameStep.isNonCombatMoveStepName(stepName)) {
      stepName = "NonCombatMove";
    } else if (GameStep.isMoveStepName(stepName)) {
      stepName = "Move";
    } else if (GameStep.isBattleStepName(stepName)) {
      stepName = "Battle";
    } else if (GameStep.isBidPlaceStepName(stepName)) {
      stepName = "BidPlace";
    } else if (GameStep.isPlaceStepName(stepName)) {
      stepName = "Place";
    } else if (GameStep.isPoliticsStepName(stepName)) {
      stepName = "Politics";
    } else if (GameStep.isEndTurnStepName(stepName)) {
      stepName = "EndTurn";
    } else {
      stepName = "";
    }
    return stepName;
  }

  private void writeProductionRulesAndUnitInfo(Writer writer) throws IOException {
    writer.append("Production Rules:").append(emptyColumnsExceptOne).append(LINE_SEPARATOR);
    final String emptyColumnsAfterProductionRules = DELIMITER.repeat(totalColumns - 5);
    writer
        .append("Name")
        .append(DELIMITER)
        .append("Result")
        .append(DELIMITER)
        .append("Quantity")
        .append(DELIMITER)
        .append("Cost")
        .append(DELIMITER)
        .append("Resource")
        .append(emptyColumnsAfterProductionRules)
        .append(LINE_SEPARATOR);

    final Collection<ProductionRule> purchaseOptions =
        gameData.getProductionRuleList().getProductionRules();
    for (final ProductionRule pr : purchaseOptions) {
      final String costString = pr.toStringCosts().replaceAll(";? ", DELIMITER);
      writer
          .append(pr.getName())
          .append(DELIMITER)
          .append(pr.getAnyResultKey().getName())
          .append(DELIMITER)
          .append(Integer.toString(pr.getResults().getInt(pr.getAnyResultKey())))
          .append(DELIMITER)
          .append(costString)
          .append(emptyColumnsAfterProductionRules)
          .append(LINE_SEPARATOR);
    }
    writer.append(LINE_SEPARATOR);

    writer.append("Unit Types:").append(emptyColumnsExceptOne).append(LINE_SEPARATOR);
    writer
        .append("Name")
        .append(DELIMITER)
        .append("Listed Abilities")
        .append(DELIMITER.repeat(totalColumns - 3))
        .append(LINE_SEPARATOR);
    for (final UnitType unitType : gameData.getUnitTypeList()) {
      final UnitAttachment ua = unitType.getUnitAttachment();
      if (ua == null) {
        continue;
      }
      final String toModify =
          ua.allUnitStatsForExporter()
              .replaceFirst("UnitType\\{name=.*?\\}", unitType.getName())
              .replaceAll("UnitType called | with:|games\\.strategy\\.engine\\.data\\.", "")
              .replaceAll("[\n,]", ";")
              .replaceAll(" {2}| ?, ?", DELIMITER);
      writer.append(toModify);
      // fill up with delimiters depending on how many are already used
      final char usedDelimiter = DELIMITER.charAt(0);
      final long delimiterCount = toModify.chars().filter(c -> usedDelimiter == c).count();
      if (delimiterCount < totalColumns) {
        writer.append(DELIMITER.repeat(totalColumns - (int) delimiterCount));
      }
      writer.append(LINE_SEPARATOR);
    }
    writer.append(LINE_SEPARATOR);
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
