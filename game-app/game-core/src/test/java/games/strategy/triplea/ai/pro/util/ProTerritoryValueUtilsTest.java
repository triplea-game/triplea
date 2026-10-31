package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ProTerritoryValueUtilsTest {
  private final GameState gameData = TestMapGameData.BIG_WORLD_1942.getGameData();
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
    assertThat(result)
        .as(
            "swUsa and northJapan are both found as they are within the min distance ("
                + ProTerritoryValueUtils.MIN_FACTORY_CHECK_DISTANCE
                + ") of newSouthWales:")
        .containsExactlyInAnyOrder(swUsa, northJapan);
  }

  @Test
  @DisplayName("only targets within the min distance are found")
  void testFindNearbyEnemyCapitalsAndFactoriesSomeWithinDistance9() {
    final var toFind = Set.of(swUsa, england, northJapan, moscow);
    final Collection<Territory> result =
        ProTerritoryValueUtils.findNearbyEnemyCapitalsAndFactories(newSouthWales, toFind);
    assertThat(result)
        .as(
            "only swUsa and northJapan are found since they are within the min distance ("
                + ProTerritoryValueUtils.MIN_FACTORY_CHECK_DISTANCE
                + ") of newSouthWales:")
        .containsExactlyInAnyOrder(swUsa, northJapan);
  }

  @Test
  @DisplayName("when no targets are within min distance, finds targets at next distance")
  void testFindNearbyEnemyCapitalsAndFactoriesNoneWithinDistance9() {
    final var toFind = Set.of(england, northJapan);
    final Collection<Territory> result =
        ProTerritoryValueUtils.findNearbyEnemyCapitalsAndFactories(newSouthWales, toFind);
    assertThat(result)
        .as(
            "only the closest target (northJapan) is found, when no targets are within "
                + "the min distance ("
                + ProTerritoryValueUtils.MIN_FACTORY_CHECK_DISTANCE
                + ") of newSouthWales:")
        .containsExactlyInAnyOrder(northJapan);
  }

  @Test
  @DisplayName("checks the computation of the max land mass size on big world")
  void testFindMaxLandMassSizeBigWorld() {
    // The result should be the same for each player since territoryCanPotentiallyMoveLandUnits()
    // should be the same for all players.
    for (final GamePlayer player : gameData.getPlayerList().getPlayers()) {
      assertThat(ProTerritoryValueUtils.findMaxLandMassSize(player)).isEqualTo(89);
    }
  }

  @Test
  @DisplayName("checks the computation of the max land mass size on revised")
  void testFindMaxLandSizeRevised() {
    final GameState gameData = TestMapGameData.REVISED.getGameData();
    for (final GamePlayer player : gameData.getPlayerList().getPlayers()) {
      assertThat(ProTerritoryValueUtils.findMaxLandMassSize(player)).isEqualTo(37);
    }
  }

  @Test
  @DisplayName("checks the computation of the max land mass size on minimap (single continent)")
  void testFindMaxLandSizeMinimap() {
    final GameState gameData = TestMapGameData.MINIMAP.getGameData();
    for (final GamePlayer player : gameData.getPlayerList().getPlayers()) {
      assertThat(ProTerritoryValueUtils.findMaxLandMassSize(player)).isEqualTo(14);
    }
  }

  @Test
  @DisplayName("sea zone value adds 100 x enemy convoy production and enemy units, by distance")
  void testFindSeaTerritoryValues() {
    final GameState gameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();
    final Territory seaZone4 = territory("4 Sea Zone", gameData);

    final double value =
        ProTerritoryValueUtils.findSeaTerritoryValues(
                russians(gameData), List.of(), List.of(seaZone4))
            .get(seaZone4);

    // Within 4 moves of the Russian 4 Sea Zone, the enemy is Germany:
    //   3 Sea Zone, 1 away: German, production 2
    //   7 Sea Zone, 3 away: 1 German unit
    //   5 Sea Zone, 3 away: German, production 2, 5 German units
    // Production: 2/2 + 2/8 = 1.25. Units: 1/8 + 5/8 = 0.75.
    assertThat(value).isEqualTo(100 * 1.25 + 0.75);
  }

  @Test
  void enemyCapitalWhoseCaptureKeepsItsIncomeIsValuedAsAFactoryOnly()
      throws MutableProperty.InvalidValueException {
    final GameData data = TestMapGameData.LHTR.getGameData();
    final PlayerAttachment pa =
        new PlayerAttachment(Constants.PLAYER_ATTACHMENT_NAME, germans(data), data);
    pa.getPropertyOrThrow("retainCapitalProduceNumber").setValue(0);
    germans(data).addAttachment(Constants.PLAYER_ATTACHMENT_NAME, pa);

    final double value =
        ProTerritoryValueUtils.findEnemyCapitalsAndFactoriesValue(
                british(data), 1, List.of(), List.of())
            .get(territory("Germany", data));

    // Germany's factory produces 10; its land mass is itself plus 23 territories within 6 moves.
    assertThat(value).isEqualTo(Math.sqrt(10) * 32 * 24);
  }
}
