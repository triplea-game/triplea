package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.TreeNode;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 * Analyzes a game's history and aggregates interesting statistics in a {@link Statistics} object.
 */
@Log
@UtilityClass
public class StatisticsAggregator {
  private static IStat productionStat = new ProductionStat();
  private static IStat tuvStat = new TuvStat();
  private static IStat unitsStat = new UnitsStat();
  private static IStat victoryCityStat = new VictoryCityStat();

  public static Statistics aggregate(@NonNull final GameData game) {
    log.info("Aggregating statistics for game " + game.getGameName());
    final Statistics underConstruction = new Statistics();

    final List<GamePlayer> players = game.getPlayerList().getPlayers();
    final List<String> alliances = new ArrayList<>(game.getAllianceTracker().getAlliances());

    for (final Round round : getRounds(game)) {
      game.getHistory().gotoNode(round);
      for (final GamePlayer player : players) {
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
      for (final String alliance : alliances) {
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
    }

    return underConstruction;
  }

  private static List<Round> getRounds(final GameData gameData) {
    final List<Round> rounds = new ArrayList<>();
    final HistoryNode root = (HistoryNode) gameData.getHistory().getRoot();
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
