package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.TreeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

/**
 * Analyzes a game's history and aggregates interesting statistics in a {@link Statistics} object.
 */
@Log
@RequiredArgsConstructor
public class StatisticsAggregator {
  private static IStat productionStat = new ProductionStat();
  private static IStat tuvStat = new TuvStat();
  private static IStat unitsStat = new UnitsStat();
  private static IStat victoryCityStat = new VictoryCityStat();

  private final Statistics underConstruction = new Statistics();
  private final GameData game;

  public Statistics aggregate() {
    log.info("Aggregating statistics for game " + game.getGameName());
    final List<GamePlayer> players = game.getPlayerList().getPlayers();
    final List<String> alliances = new ArrayList<>(game.getAllianceTracker().getAlliances());

    for (final Round round : getRounds()) {
      game.getHistory().gotoNode(round);
      for (final GamePlayer player : players) {
        processRoundOfPlayer(round, player);
      }
      for (final String alliance : alliances) {
        processRoundOfAlliance(round, alliance);
      }
    }

    return underConstruction;
  }

  private void processRoundOfAlliance(final Round round, final String alliance) {
    underConstruction
        .getProductionOfPlayerInRound()
        .put(alliance, round, productionStat.getValue(alliance, game));
    underConstruction
        .getTuvOfPlayerInRound()
        .put(alliance, round, tuvStat.getValue(alliance, game));
    underConstruction
        .getUnitsOfPlayerInRound()
        .put(alliance, round, unitsStat.getValue(alliance, game));
    underConstruction
        .getVictoryCitiesOfPlayerInRound()
        .put(alliance, round, victoryCityStat.getValue(alliance, game));
  }

  private void processRoundOfPlayer(final Round round, final GamePlayer player) {
    underConstruction
        .getProductionOfPlayerInRound()
        .put(player.getName(), round, productionStat.getValue(player, game));
    underConstruction
        .getTuvOfPlayerInRound()
        .put(player.getName(), round, tuvStat.getValue(player, game));
    underConstruction
        .getUnitsOfPlayerInRound()
        .put(player.getName(), round, unitsStat.getValue(player, game));
    underConstruction
        .getVictoryCitiesOfPlayerInRound()
        .put(player.getName(), round, victoryCityStat.getValue(player, game));
  }

  private List<Round> getRounds() {
    final List<Round> rounds = new ArrayList<>();
    final HistoryNode root = (HistoryNode) game.getHistory().getRoot();
    final Enumeration<TreeNode> rootChildren = root.children();
    while (rootChildren.hasMoreElements()) {
      final TreeNode child = rootChildren.nextElement();
      if (child instanceof Round) {
        rounds.add((Round) child);
      }
    }
    return rounds;
  }
}
