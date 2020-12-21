package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ProTerritoryValueUtilsTest {
  private final GameDataInjections gameData = TestMapGameData.BIG_WORLD_1942.getGameData();
  private final Territory swUsa = territory("Southwestern United States", gameData);
  private final Territory england = territory("England", gameData);
  private final Territory northJapan = territory("Northern Japan", gameData);
  private final Territory moscow = territory("Moscow", gameData);
  private final Territory newSouthWales = territory("New South Wales", gameData);

  @Test
  @DisplayName("all targets found when they're within the min distance")
  void testFindNearbyEnemyCapitalsAndFactoriesAllWithinDistance9() {
    final var toFind = Set.of(swUsa, northJapan);
    final Collection<Territory> result =
        ProTerritoryValueUtils.findNearbyEnemyCapitalsAndFactories(newSouthWales, toFind);
    assertThat(
        "swUsa and northJapan are both found as they are within the min distance ("
            + ProTerritoryValueUtils.MIN_FACTORY_CHECK_DISTANCE
            + ") of newSouthWales:",
        result,
        containsInAnyOrder(swUsa, northJapan));
  }

  @Test
  @DisplayName("only targets within the min distance are found")
  void testFindNearbyEnemyCapitalsAndFactoriesSomeWithinDistance9() {
    final var toFind = Set.of(swUsa, england, northJapan, moscow);
    final Collection<Territory> result =
        ProTerritoryValueUtils.findNearbyEnemyCapitalsAndFactories(newSouthWales, toFind);
    assertThat(
        "only swUsa and northJapan are found since they are within the min distance ("
            + ProTerritoryValueUtils.MIN_FACTORY_CHECK_DISTANCE
            + ") of newSouthWales:",
        result,
        containsInAnyOrder(swUsa, northJapan));
  }

  @Test
  @DisplayName("when no targets are within min distance, finds targets at next distance")
  void testFindNearbyEnemyCapitalsAndFactoriesNoneWithinDistance9() {
    final var toFind = Set.of(england, northJapan);
    final Collection<Territory> result =
        ProTerritoryValueUtils.findNearbyEnemyCapitalsAndFactories(newSouthWales, toFind);
    assertThat(
        "only the closest target (northJapan) is found, when no targets are within "
            + "the min distance ("
            + ProTerritoryValueUtils.MIN_FACTORY_CHECK_DISTANCE
            + ") of newSouthWales:",
        result,
        containsInAnyOrder(northJapan));
  }

  @Test
  @DisplayName("checks the computation of the max land mass size on big world")
  void testFindMaxLandMassSizeBigWorld() {
    // The result should be the same for each player since territoryCanPotentiallyMoveLandUnits()
    // should be the same for all players.
    for (final GamePlayer player : gameData.getPlayerList().getPlayers()) {
      assertThat(ProTerritoryValueUtils.findMaxLandMassSize(player), is(89));
    }
  }

  @Test
  @DisplayName("checks the computation of the max land mass size on revised")
  void testFindMaxLandSizeRevised() {
    final GameDataInjections gameData = TestMapGameData.REVISED.getGameData();
    for (final GamePlayer player : gameData.getPlayerList().getPlayers()) {
      assertThat(ProTerritoryValueUtils.findMaxLandMassSize(player), is(37));
    }
  }

  @Test
  @DisplayName("checks the computation of the max land mass size on minimap (single continent)")
  void testFindMaxLandSizeMinimap() {
    final GameDataInjections gameData = TestMapGameData.MINIMAP.getGameData();
    for (final GamePlayer player : gameData.getPlayerList().getPlayers()) {
      assertThat(ProTerritoryValueUtils.findMaxLandMassSize(player), is(14));
    }
  }
}
