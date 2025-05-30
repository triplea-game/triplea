package games.strategy.triplea.delegate;

import static games.strategy.triplea.Constants.SUPPORT_ATTACHMENT_PREFIX;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
import javax.annotation.Nullable;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class MatchesTest {

  private static <T> Matcher<Predicate<T>> matches(final @Nullable T value) {
    return new TypeSafeDiagnosingMatcher<>() {
      @Override
      public void describeTo(final Description description) {
        description.appendText("matcher matches using ").appendValue(value);
      }

      @Override
      public boolean matchesSafely(final Predicate<T> match, final Description description) {
        if (!match.test(value)) {
          description.appendText("it does not match");
          return false;
        }
        return true;
      }
    };
  }

  private static <T> Matcher<Predicate<T>> notMatches(final @Nullable T value) {
    return new TypeSafeDiagnosingMatcher<>() {
      @Override
      public void describeTo(final Description description) {
        description.appendText("matcher does not match using ").appendValue(value);
      }

      @Override
      public boolean matchesSafely(final Predicate<T> match, final Description description) {
        if (match.test(value)) {
          description.appendText("it matches");
          return false;
        }
        return true;
      }
    };
  }

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
      assertThat(player.isAtWar(alliedPlayer), is(false));
      enemyPlayer = GameDataTestUtil.russians(gameData);
      assertThat(player.isAtWar(enemyPlayer), is(true));

      territory = gameData.getMap().getTerritory("Germany");
      territory.setOwner(player);
      territory.getUnitCollection().clear();
    }

    @Test
    void shouldNotMatchWhenTerritoryContainsOnlyAlliedLandUnits() {
      territory.getUnitCollection().add(newLandUnitFor(alliedPlayer));

      assertThat(newMatch(), notMatches(territory));
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

      assertThat(newMatch(), matches(territory));
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

      assertThat(newMatch(), matches(territory));
    }

    @Test
    void shouldNotMatchWhenTerritoryContainsOnlyEnemyAirUnits() {
      territory.getUnitCollection().add(newAirUnitFor(enemyPlayer));

      assertThat(newMatch(), notMatches(territory));
    }

    @Test
    void shouldNotMatchWhenTerritoryContainsOnlyEnemyInfrastructureUnits() {
      territory.getUnitCollection().add(newInfrastructureUnitFor(enemyPlayer));

      assertThat(newMatch(), notMatches(territory));
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

      landTerritory = gameData.getMap().getTerritory("Germany");
      landTerritory.setOwner(player);
      assertTrue(TerritoryAttachment.get(landTerritory).isPresent());

      seaTerritory = gameData.getMap().getTerritory("Baltic Sea Zone");
      seaTerritory.setOwner(player);
      assertTrue(TerritoryAttachment.get(seaTerritory).isEmpty());
      TerritoryAttachment.add(
          seaTerritory, new TerritoryAttachment("name", seaTerritory, gameData));
      assertTrue(TerritoryAttachment.get(seaTerritory).isEmpty());
    }

    @Test
    void shouldMatchWhenLandTerritoryIsOwnedAndHasTerritoryAttachment() {
      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    void shouldMatchWhenLandTerritoryIsOwnedAndDoesNotHaveTerritoryAttachment() {
      TerritoryAttachment.remove(landTerritory);

      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    void shouldMatchWhenLandTerritoryIsUnownedAndHasTerritoryAttachment() {
      landTerritory.setOwner(gameData.getPlayerList().getNullPlayer());

      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    void shouldMatchWhenLandTerritoryIsUnownedAndDoesNotHaveTerritoryAttachment() {
      landTerritory.setOwner(gameData.getPlayerList().getNullPlayer());
      TerritoryAttachment.remove(landTerritory);

      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    void shouldMatchWhenSeaTerritoryIsOwnedAndHasTerritoryAttachment() {
      assertThat(newMatch(), matches(seaTerritory));
    }

    @Test
    void shouldMatchWhenSeaTerritoryIsOwnedAndDoesNotHaveTerritoryAttachment() {
      TerritoryAttachment.remove(seaTerritory);

      assertThat(newMatch(), matches(seaTerritory));
    }

    @Test
    void shouldMatchWhenSeaTerritoryIsUnownedAndHasTerritoryAttachment() {
      seaTerritory.setOwner(gameData.getPlayerList().getNullPlayer());

      assertThat(newMatch(), matches(seaTerritory));
    }

    @Test
    void shouldNotMatchWhenSeaTerritoryIsUnownedAndDoesNotHaveTerritoryAttachment() {
      seaTerritory.setOwner(gameData.getPlayerList().getNullPlayer());
      TerritoryAttachment.remove(seaTerritory);

      assertThat(newMatch(), notMatches(seaTerritory));
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

      assertThat(
          "An infrastructure unit normally can not be in battle",
          Matches.unitCanBeInBattle(true, true, 1, false, List.of()).test(unit),
          is(false));
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

      assertThat(
          "An infrastructure unit with attack can be in battle when it is attacking",
          Matches.unitCanBeInBattle(true, true, 1, false, List.of()).test(unit),
          is(true));
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

      assertThat(
          "An infrastructure unit with attack can not be in battle when it is attacking",
          Matches.unitCanBeInBattle(false, true, 1, false, List.of()).test(unit),
          is(false));
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

      assertThat(
          "An infrastructure unit with defense can be in battle when it is defending",
          Matches.unitCanBeInBattle(false, true, 1, false, List.of()).test(unit),
          is(true));
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

      assertThat(
          "An infrastructure unit with defense can not be in battle when it is attacking",
          Matches.unitCanBeInBattle(true, true, 1, false, List.of()).test(unit),
          is(false));
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

      assertThat(
          "An infrastructure unit that gives some support can be in battle",
          Matches.unitCanBeInBattle(true, true, 1, false, List.of()).test(unit),
          is(true));
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

      assertThat(
          "An infrastructure unit that is combat AA and can fire in the round can be "
              + "in battle",
          Matches.unitCanBeInBattle(true, true, 1, false, List.of()).test(unit),
          is(true));
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

      assertThat(
          "An infrastructure unit that is combat AA but can only fire in round 1 and "
              + "it is round 2 can not be in battle",
          Matches.unitCanBeInBattle(true, true, 2, false, List.of()).test(unit),
          is(false));
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
          "An infrastructure unit that is combat AA but can only fire in round 1 and "
              + "it is round 2 can not be in battle",
          Matches.unitCanBeInBattle(true, true, 1, false, List.of(firingUnitType)).test(unit),
          is(true));
    }
  }
}
