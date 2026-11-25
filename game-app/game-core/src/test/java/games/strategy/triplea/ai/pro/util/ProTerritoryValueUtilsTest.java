package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
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
                new ProData(false), russians(gameData), List.of(), List.of(seaZone4))
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

  @Test
  void liberatedCapitalThatCanBeHeldIsValuedForItsOwnersIncome() {
    final GameData data = TestMapGameData.LHTR.getGameData();
    territory("Russia", data).setOwner(germans(data));

    final double value =
        ProTerritoryValueUtils.findEnemyCapitalsAndFactoriesValue(
                british(data), 1, List.of(), List.of())
            .get(territory("Russia", data));

    // Russia's factory produces 8 and the Russians 16 without it; its land mass is itself plus 34
    // territories within 6 moves.
    assertThat(value).isEqualTo(Math.sqrt(8 + Math.sqrt(16)) * 32 * 35);
  }

  @Test
  void liberatedCapitalThatCantBeHeldIsValuedAsAFactoryOnly() {
    final GameData data = TestMapGameData.LHTR.getGameData();
    territory("Russia", data).setOwner(germans(data));

    final double value =
        ProTerritoryValueUtils.findEnemyCapitalsAndFactoriesValue(
                british(data), 1, List.of(territory("Russia", data)), List.of())
            .get(territory("Russia", data));

    assertThat(value).isEqualTo(Math.sqrt(8) * 32 * 35);
  }

  @Test
  @DisplayName("a victory city is worth PUs only in Hard Beta with a victory-city win condition on")
  void victoryCityValueNeedsBetaAndAVictoryCondition() {
    final GameData data = TestMapGameData.VICTORY_TEST.getGameData();
    final Territory germany = territory("Germany", data);
    final Territory eastBalkans = territory("East Balkans", data);

    setEditable(data, Constants.PROJECTION_OF_POWER, false);

    assertThat(ProTerritoryValueUtils.victoryCityValue(new ProData(true), germany))
        .as("no victory condition on")
        .isZero();

    setEditable(data, Constants.TOTAL_VICTORY, true);

    assertThat(ProTerritoryValueUtils.victoryCityValue(new ProData(true), germany))
        .isEqualTo(ProCasualtyUtils.VICTORY_CITY_VALUE);
    assertThat(ProTerritoryValueUtils.victoryCityValue(new ProData(false), germany))
        .as("stock Hard")
        .isZero();
    assertThat(ProTerritoryValueUtils.victoryCityValue(new ProData(true), eastBalkans))
        .as("not a victory city")
        .isZero();
  }

  @Test
  @DisplayName("without dice a blockade costs its points, capped at the production there")
  void blockadeLossWithoutDice() {
    assertThat(ProTerritoryValueUtils.expectedBlockadeLoss(2, 5, false)).isEqualTo(2);
    assertThat(ProTerritoryValueUtils.expectedBlockadeLoss(9, 5, false)).isEqualTo(5);
    assertThat(ProTerritoryValueUtils.expectedBlockadeLoss(0, 5, false)).isZero();
    assertThat(ProTerritoryValueUtils.expectedBlockadeLoss(3, 0, false)).isZero();
  }

  @Test
  @DisplayName("a rolled blockade point is worth 1 PU on average, less once the cap bites")
  void blockadeLossWithDice() {
    assertThat(ProTerritoryValueUtils.expectedBlockadeLoss(1, 10, true)).isEqualTo(1.0);
    assertThat(ProTerritoryValueUtils.expectedBlockadeLoss(4, 100, true))
        .isCloseTo(4.0, org.assertj.core.data.Offset.offset(1e-9));
    // (1+2+3)/6 on one die, but only 1 PU is at stake: 1/6 * 1 + 1/6 * 1 + 1/6 * 1
    assertThat(ProTerritoryValueUtils.expectedBlockadeLoss(1, 1, true))
        .isCloseTo(0.5, org.assertj.core.data.Offset.offset(1e-9));
  }

  @Test
  @DisplayName("a blockade zone is worth the production an enemy blockader there would cut")
  void blockadeValueDefendsAdjacentIncome() throws Exception {
    final GameData data = TestMapGameData.BIG_WORLD_1942.getGameData();
    final GamePlayer british = british(data);
    final Territory zone =
        data.getMap().getNeighbors(territory("England", data)).stream()
            .filter(Territory::isWater)
            .findFirst()
            .orElseThrow();
    final var attachment = new TerritoryAttachment("blockade", zone, data);
    zone.addAttachment(Constants.TERRITORY_ATTACHMENT_NAME, attachment);
    attachment.getPropertyOrThrow("blockadeZone").setValue(true);
    submarine(data).getUnitAttachment().getPropertyOrThrow("blockade").setValue(2);
    for (final Territory t : data.getMap().getTerritories()) {
      removeFrom(t, t.getUnitCollection().getMatches(Matches.unitIsOwnedBy(british)));
    }
    addTo(zone, submarine(data).create(1, germans(data)));

    final ProData beta = new ProData(true);
    final int ourBlockade = ProTerritoryValueUtils.ourBlockadePoints(beta, british);

    assertThat(ProTerritoryValueUtils.blockadeValue(beta, british, zone, ourBlockade))
        .isEqualTo(2.0);
    assertThat(ProTerritoryValueUtils.blockadeValue(new ProData(false), british, zone, ourBlockade))
        .as("stock Hard")
        .isZero();
  }

  @Test
  @DisplayName("each enemy bordering a blockade zone can lose up to our full blockade")
  void blockadeValueCapsTheLossPerEnemy() throws Exception {
    final GameData data = TestMapGameData.BIG_WORLD_1942.getGameData();
    final Territory channel = territory("SZ 17 English Channel", data);
    final Territory france = territory("France", data);
    final var attachment = new TerritoryAttachment("blockade", channel, data);
    channel.addAttachment(Constants.TERRITORY_ATTACHMENT_NAME, attachment);
    attachment.getPropertyOrThrow("blockadeZone").setValue(true);
    removeFrom(france, france.getUnits());
    france.setOwner(russians(data));

    // England (British, 4 PUs) and France (Russian, 3 PUs) border the channel
    assertThat(ProTerritoryValueUtils.blockadeValue(new ProData(true), germans(data), channel, 3))
        .isEqualTo(3.0 + 3.0);
  }

  @SuppressWarnings("unchecked")
  private static void setEditable(final GameData data, final String key, final boolean value) {
    ((IEditableProperty<Object>) data.getProperties().getEditablePropertiesByName().get(key))
        .setValue(value);
  }
}
