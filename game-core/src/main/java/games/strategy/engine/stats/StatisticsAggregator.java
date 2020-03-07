package games.strategy.engine.stats;

import com.google.common.collect.HashBasedTable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.triplea.ui.AbstractStatPanel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.tree.TreeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

/**
 * Analyzes a game's history and aggregates interesting statistics in a {@link Statistics} object.
 */
@Log
@RequiredArgsConstructor
public class StatisticsAggregator {
  private final Statistics underConstruction = new Statistics();
  private final GameData game;

  private static final Map<OverTimeStatisticType, IStat> defaultStatisticsMapping =
      Map.of(
          OverTimeStatisticType.PredefinedStatistics.TUV, new TuvStat(),
          OverTimeStatisticType.PredefinedStatistics.PRODUCTION, new ProductionStat(),
          OverTimeStatisticType.PredefinedStatistics.UNITS, new UnitsStat(),
          OverTimeStatisticType.PredefinedStatistics.VC, new VictoryCityStat());

  private static Map<OverTimeStatisticType, IStat> createOverTimeStatisticsMapping(
      List<Resource> resources) {
    final Map<OverTimeStatisticType, IStat> statisticsMapping =
        new HashMap<>(defaultStatisticsMapping);
    resources.forEach(
        resource ->
            statisticsMapping.put(
                new OverTimeStatisticType.ResourceStatistic(resource),
                new AbstractStatPanel.ResourceStat(resource)));
    return statisticsMapping;
  }

  public Statistics aggregate() {
    log.info("Aggregating statistics for game " + game.getGameName());
    final List<GamePlayer> players = game.getPlayerList().getPlayers();
    final List<String> alliances = new ArrayList<>(game.getAllianceTracker().getAlliances());

    final Map<OverTimeStatisticType, IStat> overTimeStatisticToSource =
        createOverTimeStatisticsMapping(game.getResourceList().getResources());
    {
      // initialize over time statistics
      for (OverTimeStatisticType type : overTimeStatisticToSource.keySet()) {
        underConstruction.getOverTimeStatistics().put(type, HashBasedTable.create());
      }
    }

    for (final Round round : getRounds()) {
      game.getHistory().gotoNode(round);
      for (final GamePlayer player : players) {
        overTimeStatisticToSource.forEach(
            (type, source) ->
                underConstruction
                    .getOverTimeStatistics()
                    .get(type)
                    .put(player.getName(), round, source.getValue(player, game)));
      }
      for (final String alliance : alliances) {
        overTimeStatisticToSource.forEach(
            (type, source) ->
                underConstruction
                    .getOverTimeStatistics()
                    .get(type)
                    .put(alliance, round, source.getValue(alliance, game)));
      }
    }

    return underConstruction;
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
