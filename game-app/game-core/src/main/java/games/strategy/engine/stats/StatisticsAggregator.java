package games.strategy.engine.stats;

import com.google.common.collect.HashBasedTable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.triplea.ui.mapdata.MapData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.tree.TreeNode;
import lombok.extern.slf4j.Slf4j;

/**
 * Analyzes a game's history and aggregates interesting statistics in a {@link Statistics} object.
 */
@Slf4j
public class StatisticsAggregator {
  private static final Map<OverTimeStatisticType, IStat> defaultStatisticsMapping =
      Map.of(
          OverTimeStatisticType.PredefinedStatistics.TUV, new TuvStat(),
          OverTimeStatisticType.PredefinedStatistics.PRODUCTION, new ProductionStat(),
          OverTimeStatisticType.PredefinedStatistics.UNITS, new UnitsStat(),
          OverTimeStatisticType.PredefinedStatistics.VC, new VictoryCityStat());
  private final Statistics underConstruction = new Statistics();
  private final GameData game;
  private final MapData mapData;

  public StatisticsAggregator(GameData gameData, MapData map) {
    this.game = GameDataUtils.cloneGameDataWithHistory(gameData, true);
    this.mapData = map;
  }

  private static Map<OverTimeStatisticType, IStat> createOverTimeStatisticsMapping(
      final Collection<Resource> resources) {
    final Map<OverTimeStatisticType, IStat> statisticsMapping =
        new HashMap<>(defaultStatisticsMapping);
    resources.forEach(
        resource ->
            statisticsMapping.put(
                new OverTimeStatisticType.ResourceStatistic(resource), new ResourceStat(resource)));
    return statisticsMapping;
  }

  public Statistics aggregate() {
    log.info("Aggregating statistics for game " + game.getGameName());
    collectOverTimeStatistics();
    return underConstruction;
  }

  private void collectOverTimeStatistics() {
    final Map<OverTimeStatisticType, IStat> overTimeStatisticSources =
        createOverTimeStatisticsMapping(game.getResourceList().getResources());
    {
      // initialize over time statistics
      for (final OverTimeStatisticType type : overTimeStatisticSources.keySet()) {
        underConstruction.getOverTimeStatistics().put(type, HashBasedTable.create());
      }
    }

    final List<GamePlayer> players = game.getPlayerList().getPlayers();
    final List<String> alliances = new ArrayList<>(game.getAllianceTracker().getAlliances());
    for (final Round round : getRounds()) {
      game.getHistory().gotoNode(round);
      collectOverTimeStatisticsForRound(overTimeStatisticSources, players, alliances, round);
    }
  }

  private void collectOverTimeStatisticsForRound(
      final Map<OverTimeStatisticType, IStat> overTimeStatisticSources,
      final List<GamePlayer> players,
      final List<String> alliances,
      final Round round) {
    for (final GamePlayer player : players) {
      overTimeStatisticSources.forEach(
          (type, source) ->
              underConstruction
                  .getOverTimeStatistics()
                  .get(type)
                  .put(player.getName(), round, source.getValue(player, game, mapData)));
    }
    for (final String alliance : alliances) {
      overTimeStatisticSources.forEach(
          (type, source) ->
              underConstruction
                  .getOverTimeStatistics()
                  .get(type)
                  .put(alliance, round, source.getValue(alliance, game, mapData)));
    }
  }

  private List<Round> getRounds() {
    final List<Round> rounds = new ArrayList<>();
    final HistoryNode root = (HistoryNode) game.getHistory().getRoot();
    final Enumeration<TreeNode> rootChildren = root.children();
    while (rootChildren.hasMoreElements()) {
      final TreeNode child = rootChildren.nextElement();
      if (child instanceof Round childRound) {
        rounds.add(childRound);
      }
    }
    return rounds;
  }
}
