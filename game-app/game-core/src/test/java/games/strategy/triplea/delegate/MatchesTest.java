package games.strategy.triplea.delegate;

import static games.strategy.triplea.Constants.SUPPORT_ATTACHMENT_PREFIX;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class MatchesTest {

  @Nested
  final class TerritoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemyTest {
    private GameState gameData;
    private GamePlayer player;
    private GamePlayer alliedPlayer;
    private GamePlayer enemyPlayer;
    private Territory territory;

    private Predicate<Territory> newMatch() {
      return Matches.territoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemy(player);
    }

    private Unit newAirUnitFor(final GamePlayer player) {
      return GameDataTestUtil.fighter(gameData).create(player);
    }

    private Unit newInfrastructureUnitFor(final GamePlayer player) {
      return GameDataTestUtil.aaGun(gameData).create(player);
    }

    private Unit newLandUnitFor(final GamePlayer player) {
      return GameDataTestUtil.infantry(gameData).create(player);
    }

    private Unit newSeaUnitFor(final GamePlayer player) {
      return GameDataTestUtil.battleship(gameData).create(player);
    }

    @BeforeEach
    void setUp() {
      gameData = TestMapGameData.DELEGATE_TEST.getGameData();

      player = GameDataTestUtil.germans(gameData);
      alliedPlayer = GameDataTestUtil.japanese(gameData);
      assertThat(player.isAtWar(alliedPlayer)).isFalse();
      enemyPlayer = GameDataTestUtil.russians(gameData);
      assertThat(player.isAtWar(enemyPlayer)).isTrue();

      territory = gameData.getMap().getTerritoryOrNull("Germany");
      territory.setOwner(player);
      territory.getUnitCollection().clear();
    }

    @Test
    void shouldNotMatchWhenTerritoryContainsOnlyAlliedLandUnits() {
      territory.getUnitCollection().add(newLandUnitFor(alliedPlayer));

      assertThat(newMatch().test(territory)).isFalse();
    }

    @Test
    void shouldMatchWhenTerritoryContainsEnemyLandUnits() {
      territory
          .getUnitCollection()
          .addAll(
              List.of(
                  newLandUnitFor(player),
                  newLandUnitFor(enemyPlayer),
                  newAirUnitFor(enemyPlayer),
                  newInfrastructureUnitFor(enemyPlayer)));

      assertThat(newMatch().test(territory)).isTrue();
    }

    @Test
    void shouldMatchWhenTerritoryContainsEnemySeaUnits() {
      territory
          .getUnitCollection()
          .addAll(
              List.of(
                  newSeaUnitFor(player),
                  newSeaUnitFor(enemyPlayer),
                  newAirUnitFor(enemyPlayer),
                  newInfrastructureUnitFor(enemyPlayer)));

      assertThat(newMatch().test(territory)).isTrue();
    }

    @Test
    void shouldNotMatchWhenTerritoryContainsOnlyEnemyAirUnits() {
      territory.getUnitCollection().add(newAirUnitFor(enemyPlayer));

      assertThat(newMatch().test(territory)).isFalse();
    }

    @Test
    void shouldNotMatchWhenTerritoryContainsOnlyEnemyInfrastructureUnits() {
      territory.getUnitCollection().add(newInfrastructureUnitFor(enemyPlayer));

      assertThat(newMatch().test(territory)).isFalse();
    }
  }

  @Nested
  final class TerritoryIsNotUnownedWaterTest {
    private GameData gameData;
    private GamePlayer player;
    private Territory landTerritory;
    private Territory seaTerritory;

    private Predicate<Territory> newMatch() {
      return Matches.territoryIsNotUnownedWater();
    }

    @BeforeEach
    void setUp() {
      gameData = TestMapGameData.DELEGATE_TEST.getGameData();

      player = GameDataTestUtil.germans(gameData);

      landTerritory = gameData.getMap().getTerritoryOrNull("Germany");
      landTerritory.setOwner(player);
      assertTrue(TerritoryAttachment.get(landTerritory).isPresent());

      seaTerritory = gameData.getMap().getTerritoryOrNull("Baltic Sea Zone");
      seaTerritory.setOwner(player);
      assertTrue(TerritoryAttachment.get(seaTerritory).isEmpty());
      TerritoryAttachment.add(
          seaTerritory, new TerritoryAttachment("name", seaTerritory, gameData));
      assertTrue(TerritoryAttachment.get(seaTerritory).isPresent());
    }

    @Test
    void shouldMatchWhenLandTerritoryIsOwnedAndHasTerritoryAttachment() {
      assertThat(newMatch().test(landTerritory)).isTrue();
    }

    @Test
    void shouldMatchWhenLandTerritoryIsOwnedAndDoesNotHaveTerritoryAttachment() {
      TerritoryAttachment.remove(landTerritory);

      assertThat(newMatch().test(landTerritory)).isTrue();
    }

    @Test
    void shouldMatchWhenLandTerritoryIsUnownedAndHasTerritoryAttachment() {
      landTerritory.setOwner(gameData.getPlayerList().getNullPlayer());

      assertThat(newMatch().test(landTerritory)).isTrue();
    }

    @Test
    void shouldMatchWhenLandTerritoryIsUnownedAndDoesNotHaveTerritoryAttachment() {
      landTerritory.setOwner(gameData.getPlayerList().getNullPlayer());
      TerritoryAttachment.remove(landTerritory);

      assertThat(newMatch().test(landTerritory)).isTrue();
    }

    @Test
    void shouldMatchWhenSeaTerritoryIsOwnedAndHasTerritoryAttachment() {
      assertThat(newMatch().test(seaTerritory)).isTrue();
    }

    @Test
    void shouldMatchWhenSeaTerritoryIsOwnedAndDoesNotHaveTerritoryAttachment() {
      TerritoryAttachment.remove(seaTerritory);

      assertThat(newMatch().test(seaTerritory)).isTrue();
    }

    @Test
    void shouldMatchWhenSeaTerritoryIsUnownedAndHasTerritoryAttachment() {
      seaTerritory.setOwner(gameData.getPlayerList().getNullPlayer());

      assertThat(newMatch().test(seaTerritory)).isTrue();
    }

    @Test
    void shouldNotMatchWhenSeaTerritoryIsUnownedAndDoesNotHaveTerritoryAttachment() {
      seaTerritory.setOwner(gameData.getPlayerList().getNullPlayer());
      TerritoryAttachment.remove(seaTerritory);

      assertThat(newMatch().test(seaTerritory)).isFalse();
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  final class UnitCanBeInBattle {

    @Mock GamePlayer player;
    GameData gameData;

    @BeforeEach
    void setupGameData() {
      gameData = givenGameData().build();
    }

    @Test
    void infrastructureShouldNormallyNotBeInBattle() {
      when(gameData.getDiceSides()).thenReturn(6);
      final UnitType unitType = new UnitType("infrastructure", gameData);
      final UnitAttachment unitAttachment =
          new UnitAttachment("infrastructure", unitType, gameData);
      unitAttachment.setIsInfrastructure(true);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);

      assertThat(Matches.unitCanBeInBattle(true, true, 1, false, List.of()).test(unit))
          .as("An infrastructure unit normally can not be in battle")
          .isFalse();
    }

    @Test
    void infrastructureWithAttackCanBeInBattleWhenAttacking() {
      when(gameData.getDiceSides()).thenReturn(6);
      final UnitType unitType = new UnitType("infrastructure", gameData);
      final UnitAttachment unitAttachment =
          new UnitAttachment("infrastructure", unitType, gameData);
      unitAttachment.setIsInfrastructure(true);
      unitAttachment.setAttack(1);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);

      assertThat(Matches.unitCanBeInBattle(true, true, 1, false, List.of()).test(unit))
          .as("An infrastructure unit with attack can be in battle when it is attacking")
          .isTrue();
    }

    @Test
    void infrastructureWithAttackCanNotBeInBattleWhenDefending() {
      when(gameData.getDiceSides()).thenReturn(6);
      final UnitType unitType = new UnitType("infrastructure", gameData);
      final UnitAttachment unitAttachment =
          new UnitAttachment("infrastructure", unitType, gameData);
      unitAttachment.setIsInfrastructure(true);
      unitAttachment.setAttack(1);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);

      assertThat(Matches.unitCanBeInBattle(false, true, 1, false, List.of()).test(unit))
          .as("An infrastructure unit with attack can not be in battle when it is attacking")
          .isFalse();
    }

    @Test
    void infrastructureWithDefenseCanBeInBattleWhenDefending() {
      when(gameData.getDiceSides()).thenReturn(6);
      final UnitType unitType = new UnitType("infrastructure", gameData);
      final UnitAttachment unitAttachment =
          new UnitAttachment("infrastructure", unitType, gameData);
      unitAttachment.setIsInfrastructure(true);
      unitAttachment.setDefense(1);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);

      assertThat(Matches.unitCanBeInBattle(false, true, 1, false, List.of()).test(unit))
          .as("An infrastructure unit with defense can be in battle when it is defending")
          .isTrue();
    }

    @Test
    void infrastructureWithDefenseCanNotBeInBattleWhenAttacking() {
      when(gameData.getDiceSides()).thenReturn(6);
      final UnitType unitType = new UnitType("infrastructure", gameData);
      final UnitAttachment unitAttachment =
          new UnitAttachment("infrastructure", unitType, gameData);
      unitAttachment.setIsInfrastructure(true);
      unitAttachment.setDefense(1);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);

      assertThat(Matches.unitCanBeInBattle(true, true, 1, false, List.of()).test(unit))
          .as("An infrastructure unit with defense can not be in battle when it is attacking")
          .isFalse();
    }

    @Test
    void infrastructureThatGivesAnyTypeOfSupportCanBeInBattle() {
      final UnitType unitType = new UnitType("infrastructure", gameData);
      final UnitAttachment unitAttachment =
          new UnitAttachment("infrastructure", unitType, gameData);
      unitAttachment.setIsInfrastructure(true);
      final UnitSupportAttachment unitSupportAttachment =
          new UnitSupportAttachment(SUPPORT_ATTACHMENT_PREFIX + "support", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      unitType.addAttachment(SUPPORT_ATTACHMENT_PREFIX, unitSupportAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);

      assertThat(Matches.unitCanBeInBattle(true, true, 1, false, List.of()).test(unit))
          .as("An infrastructure unit that gives some support can be in battle")
          .isTrue();
    }

    @Test
    void infrastructureThatIsAaForCombatCanBeInBattleAndCanFireCanBeInTheBattle() {
      final UnitType unitType = new UnitType("infrastructure", gameData);
      final UnitAttachment unitAttachment =
          new UnitAttachment("infrastructure", unitType, gameData);
      unitAttachment.setIsInfrastructure(true);
      unitAttachment.setIsAaForCombatOnly(true);
      unitAttachment.setMaxRoundsAa(1);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);

      assertThat(Matches.unitCanBeInBattle(true, true, 1, false, List.of()).test(unit))
          .as(
              "An infrastructure unit that is combat AA and can fire in the round can be "
                  + "in battle")
          .isTrue();
    }

    @Test
    void infrastructureThatIsAaForCombatCanBeInBattleAndCanNotFireCanNotBeInTheBattle() {
      final UnitType unitType = new UnitType("infrastructure", gameData);
      final UnitAttachment unitAttachment =
          new UnitAttachment("infrastructure", unitType, gameData);
      unitAttachment.setIsInfrastructure(true);
      unitAttachment.setIsAaForCombatOnly(true);
      unitAttachment.setMaxRoundsAa(1);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);

      assertThat(Matches.unitCanBeInBattle(true, true, 2, false, List.of()).test(unit))
          .as(
              "An infrastructure unit that is combat AA but can only fire in round 1 and "
                  + "it is round 2 can not be in battle")
          .isFalse();
    }

    @Test
    void infrastructureThatIsAnAaTargetCanBeInBattle() {
      final UnitType unitType = new UnitType("infrastructure", gameData);
      final UnitAttachment unitAttachment =
          new UnitAttachment("infrastructure", unitType, gameData);
      unitAttachment.setIsInfrastructure(true);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);

      final UnitType firingUnitType = new UnitType("firingAa", gameData);
      final UnitAttachment firingUnitAttachment =
          new UnitAttachment("firingAa", firingUnitType, gameData);
      firingUnitAttachment.setTargetsAa(Set.of(unitType));
      firingUnitAttachment.setIsAaForCombatOnly(true);
      firingUnitAttachment.setMaxRoundsAa(1);
      firingUnitType.addAttachment(UNIT_ATTACHMENT_NAME, firingUnitAttachment);

      assertThat(
              Matches.unitCanBeInBattle(true, true, 1, false, List.of(firingUnitType)).test(unit))
          .as(
              "An infrastructure unit that is combat AA but can only fire in round 1 and "
                  + "it is round 2 can not be in battle")
          .isTrue();
    }
  }
}
