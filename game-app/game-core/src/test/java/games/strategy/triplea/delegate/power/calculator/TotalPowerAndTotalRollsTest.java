package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.dice.calculator.RolledDice;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.collections.IntegerMap;

@ExtendWith(MockitoExtension.class)
class TotalPowerAndTotalRollsTest {

  private GamePlayer givenPlayer(GameData gameData) {
    GamePlayer player = mock(GamePlayer.class);
    lenient().when(player.getData()).thenReturn(gameData);
    lenient().when(player.getTechAttachment()).thenReturn(mock(TechAttachment.class));
    return player;
  }

  private Unit givenUnit(String name, GamePlayer owner) {
    return givenUnit(givenUnitType(name, owner.getData()), owner);
  }

  private Unit givenUnit(UnitType unitType, GamePlayer owner) {
    return unitType.createTemp(1, owner).get(0);
  }

  private UnitType givenUnitType(final String name, final GameData gameData) {
    final UnitType unitType = new UnitType(name + "Type", gameData);
    final UnitAttachment unitAttachment =
        new UnitAttachment(name + "Attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    return unitType;
  }

  UnitSupportAttachment givenUnitSupportAttachment(
      final GamePlayer player, final UnitType unitType, final String name, final String diceType)
      throws GameParseException {
    return new UnitSupportAttachment("rule" + name, unitType, player.getData())
        .setBonus(1)
        .setBonusType("bonus" + name)
        .setDice(diceType)
        .setNumber(1)
        .setPlayers(List.of(player))
        .setSide("offence")
        .setFaction("allied");
  }

  @Nested
  class GetTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack {

    @Test
    void singleAaWithOneRoll() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("Total Power equals the single unit's strength")
          .isEqualTo(2);
      assertThat(result.isSameStrength()).as("Only one unit, so only one strength").isTrue();
      assertThat(sortedDie).isEqualTo(List.of(new Die(1, 2, Die.DieType.HIT)));
    }

    private AaPowerStrengthAndRolls whenGetPowerHitsResult(
        final List<Unit> units,
        final List<Die> sortedDie,
        final int dieHit,
        final int numValidTargets) {
      final AaPowerStrengthAndRolls unitPowerAndRollsMap =
          AaPowerStrengthAndRolls.build(
              units,
              numValidTargets,
              AaOffenseCombatValue.builder()
                  .rollSupportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      final int totalRolls = unitPowerAndRollsMap.calculateTotalRolls();
      final int[] dice = new int[totalRolls];
      for (int i = 0; i < totalRolls; i++) {
        dice[i] = dieHit;
      }

      sortedDie.addAll(RolledDice.getDiceHits(dice, unitPowerAndRollsMap.getActiveUnits()));
      return unitPowerAndRollsMap;
    }

    @Test
    void singleAaWithOneRollNoHit() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      whenGetPowerHitsResult(units, sortedDie, 6, 4);

      assertThat(sortedDie)
          .as("The strength was 2 but the dice rolled a 6 so it was a miss")
          .isEqualTo(List.of(new Die(6, 2, Die.DieType.MISS)));
    }

    @Test
    void singleAaWithTwoRoll() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("2 strength in 2 rolls equals total power of 4")
          .isEqualTo(4);
      assertThat(result.isSameStrength()).as("Only one unit, so only one strength").isTrue();
      assertThat(sortedDie)
          .isEqualTo(List.of(new Die(1, 2, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void singleAaWithMoreRollsThanTargets() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(3);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 2);

      assertThat(result.calculateTotalPower())
          .as("Unit has 3 rolls but only 2 targets, so 2 rolls of 2 strength = 4")
          .isEqualTo(4);
      assertThat(result.isSameStrength()).as("Only one unit, so only one strength").isTrue();
      assertThat(sortedDie)
          .isEqualTo(List.of(new Die(1, 2, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void twoAaWithSamePower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower()).as("2 strength + 2 strength is 4").isEqualTo(4);
      assertThat(result.isSameStrength()).as("Both units have the same strength").isTrue();
      assertThat(sortedDie)
          .isEqualTo(List.of(new Die(1, 2, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void twoAaWithDifferentPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);

      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower()).as("2 strength + 3 strength is 5").isEqualTo(5);
      assertThat(result.isSameStrength()).as("Both units have different strength values").isFalse();
      assertThat(sortedDie)
          .isEqualTo(List.of(new Die(1, 3, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void twoAaWithDifferentPowerAndMoreRollsThanTargets() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2);

      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 3);

      assertThat(result.calculateTotalPower())
          .as(
              "The second unit has higher strength so it rolls both "
                  + "and the first unit only rolls once. 3 * 2 + 2")
          .isEqualTo(8);
      assertThat(sortedDie)
          .as("First two dice are from the second stronger unit")
          .isEqualTo(
              List.of(
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void twoAaWithDifferentPowerAndOnlyOneHit() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      whenGetPowerHitsResult(units, sortedDie, 2, 4);

      assertThat(sortedDie)
          .as(
              "The dice is a 2 so the first unit hits (with a strength of 3) "
                  + "but the second misses (with a strength of 2). "
                  + "Strength is 1 based and the dice value is 0 based.")
          .isEqualTo(List.of(new Die(2, 3, Die.DieType.HIT), new Die(2, 2, Die.DieType.MISS)));
    }

    @Test
    void oneAaWithInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("Infinite strength of 2 is multiplied by the rolls so 8")
          .isEqualTo(8);
      assertThat(result.isSameStrength()).as("Only one unit, so only one strength").isTrue();
      assertThat(sortedDie)
          .isEqualTo(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void twoAaWithInfiniteWithSamePower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("Two infinite units are equal to one infinite unit")
          .isEqualTo(8);
      assertThat(result.isSameStrength())
          .as("Only one infinite unit is used and it always has the same strength")
          .isTrue();
      assertThat(sortedDie)
          .isEqualTo(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void twoAaWithInfiniteWithDifferentPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("The strongest infinite unit is used for all targets")
          .isEqualTo(12);
      assertThat(result.isSameStrength())
          .as("Only one infinite unit and it always has the same strength")
          .isTrue();
      assertThat(sortedDie)
          .isEqualTo(
              List.of(
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT)));
    }

    @Test
    void twoAaWithInfiniteWithDifferentDice() {
      final GameData gameData = givenGameData().build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(-1)
          .setOffensiveAttackAaMaxDieSides(4);
      final Unit unit2 = givenUnit("test2", player);
      unit2
          .getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(-1)
          .setOffensiveAttackAaMaxDieSides(8);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("2 of 4 is better than 3 of 8 so the 2 strength is used for all targets")
          .isEqualTo(8);
      assertThat(result.isSameStrength())
          .as("Only one infinite unit and it always has the same strength")
          .isTrue();
      assertThat(sortedDie)
          .as("2 of 4 is better than 3 of 8 so that is used for strength and dice sides")
          .isEqualTo(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void twoAaWithOneRollAndInfiniteSamePower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("Both units have strength 2 so the power is 2 * 4 (rolls) = 8")
          .isEqualTo(8);
      assertThat(result.isSameStrength()).as("Both units have the same strength").isTrue();
      assertThat(sortedDie)
          .isEqualTo(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void twoAaWithOneRollAndInfiniteWhereInfiniteIsHigher() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("The non infinite unit is not used so the power is 3 (strength) * 4 (roll)")
          .isEqualTo(12);
      assertThat(result.isSameStrength())
          .as("The non infinite unit is not used so the infinite unit always has the same strength")
          .isTrue();
      assertThat(sortedDie)
          .as("The non infinite unit is not used")
          .isEqualTo(
              List.of(
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT)));
    }

    @Test
    void twoAaWithOneRollAndInfiniteWhereInfiniteIsLower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("The non infinite unit is used once so 3 + 2 * 3")
          .isEqualTo(9);
      assertThat(result.isSameStrength())
          .as("The non infinite has a higher strength than the infinite so both are used")
          .isFalse();
      assertThat(sortedDie)
          .as("The non infinite unit is used first")
          .isEqualTo(
              List.of(
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void oneAaWithOverStack() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("2 rolls with 2 strength is 4 power")
          .isEqualTo(4);
      assertThat(result.isSameStrength()).as("Only one unit, so only one strength").isTrue();
      assertThat(sortedDie)
          .isEqualTo(List.of(new Die(1, 2, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void oneAaWithOverStackAndMoreRollsThanTargets() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(5).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("Unit has 5 rolls with 2 strength and can overstack, so 5 * 2")
          .isEqualTo(10);
      assertThat(result.isSameStrength()).as("Only one unit, so only one strength").isTrue();
      assertThat(sortedDie)
          .isEqualTo(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void oneAaWithOverstackAndInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 2);

      assertThat(result.calculateTotalPower())
          .as(
              "Overstack makes no sense on an infinite unit. "
                  + "Unit gets 1 roll for each target: 2 (roll) * 2 (strength)")
          .isEqualTo(4);
      assertThat(result.isSameStrength()).isTrue();
      assertThat(sortedDie)
          .isEqualTo(List.of(new Die(1, 2, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void oneOverstackAndOneInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as("Infinite unit hits all 4, overstack unit adds 2 more: 6 (roll) * 2 (strength)")
          .isEqualTo(12);
      assertThat(result.isSameStrength()).as("Both units have the same strength").isTrue();
      assertThat(sortedDie)
          .as("Infiniteunit hits all 4, overstack unit adds 2 more")
          .isEqualTo(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void oneOverstackAndOneInfiniteDifferentPowers() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower())
          .as(
              "Infinite unit hits all 4 with strength 2, "
                  + "overstack unit adds 2 more with strength 3: 4 * 2 + 3 * 2")
          .isEqualTo(14);
      assertThat(result.isSameStrength()).as("Both units have different strength").isFalse();
      assertThat(sortedDie)
          .as("Infinite unit hits all 4, overstack unit adds 2 more at the end")
          .isEqualTo(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT)));
    }

    @Test
    void oneOverstackAndOneNormal() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower()).isEqualTo(8);
      assertThat(result.isSameStrength()).isTrue();
      assertThat(sortedDie)
          .as("Overstack adds more rolls")
          .isEqualTo(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void oneOverstackAndOneNormalDifferentPowers() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower()).isEqualTo(10);
      assertThat(result.isSameStrength()).isFalse();
      assertThat(sortedDie)
          .as("Overstack adds more rolls at the end")
          .isEqualTo(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT)));
    }

    @Test
    void oneOverstackOneInfiniteAndOneNormalSamePower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test3", player);
      unit3.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2, unit3);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower()).isEqualTo(12);
      assertThat(result.isSameStrength()).isTrue();
      assertThat(sortedDie)
          .as("Overstack adds more rolls")
          .isEqualTo(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void oneOverstackOneInfiniteAndOneNormalDifferentPowersWhereNormalIsBest() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test3", player);
      unit3.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2, unit3);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower()).isEqualTo(18);
      assertThat(result.isSameStrength()).isFalse();
      assertThat(sortedDie)
          .as("Overstack adds more rolls")
          .isEqualTo(
              List.of(
                  new Die(1, 4, Die.DieType.HIT),
                  new Die(1, 4, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT)));
    }

    @Test
    void oneOverstackOneInfiniteAndOneNormalDifferentPowersWhereNormalIsWorst() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test3", player);
      unit3.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2, unit3);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result = whenGetPowerHitsResult(units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower()).isEqualTo(20);
      assertThat(result.isSameStrength()).isFalse();
      assertThat(sortedDie)
          .as("Overstack adds more rolls")
          .isEqualTo(
              List.of(
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 4, Die.DieType.HIT),
                  new Die(1, 4, Die.DieType.HIT)));
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class SortAaHighToLowTest {

    private final GameData gameData = TestMapGameData.LHTR.getGameData();

    private Unit unit1;
    private Unit unit2;
    private Unit unit3;
    private Unit unit4;
    private Unit unit5;

    private final List<Unit> units = new ArrayList<>();

    @BeforeEach
    void setUp() {
      final GamePlayer player = givenPlayer(gameData);
      unit1 = givenUnit("test1", player);
      unit2 = givenUnit("test2", player);
      unit3 = givenUnit("test3", player);
      unit4 = givenUnit("test4", player);
      unit5 = givenUnit("test5", player);
      units.addAll(List.of(unit1, unit2, unit3, unit4, unit5));
    }

    @Test
    void testAttacking() {
      int index = 4;
      for (final var unit : units) {
        // We're integer dividing the index at this point to get duplicate sorting keys
        // in order to reach some edge cases
        final UnitAttachment unitAttachment = unit.getUnitAttachment();
        unitAttachment.setOffensiveAttackAa(index / 2).setMaxAaAttacks(1);
        index--;
      }
      final List<Unit> sortedUnits =
          units.stream()
              .sorted(
                  CombatValueBuilder.aaCombatValue()
                      .enemyUnits(List.of())
                      .friendlyUnits(List.of())
                      .side(BattleState.Side.OFFENSE)
                      .supportAttachments(List.of())
                      .build()
                      .unitComparator())
              .collect(Collectors.toList());
      assertThat(sortedUnits.get(0)).isEqualTo(unit1);
      assertThat(sortedUnits.get(1)).isEqualTo(unit2);
      assertThat(sortedUnits.get(2)).isEqualTo(unit3);
      assertThat(sortedUnits.get(3)).isEqualTo(unit4);
      assertThat(sortedUnits.get(4)).isEqualTo(unit5);
    }

    @Test
    void testDefending() {
      int index = 0;
      for (final var unit : units) {
        // We're integer dividing the index at this point to get duplicate sorting keys
        // in order to reach some edge cases
        final UnitAttachment unitAttachment = unit.getUnitAttachment();
        unitAttachment.setAttackAa(index / 2).setMaxAaAttacks(1);
        index++;
      }
      final List<Unit> sortedUnits =
          units.stream()
              .sorted(
                  CombatValueBuilder.aaCombatValue()
                      .enemyUnits(List.of())
                      .friendlyUnits(List.of())
                      .side(BattleState.Side.DEFENSE)
                      .supportAttachments(List.of())
                      .build()
                      .unitComparator())
              .collect(Collectors.toList());
      assertThat(sortedUnits.get(0)).isEqualTo(unit5);
      assertThat(sortedUnits.get(1)).isEqualTo(unit3);
      assertThat(sortedUnits.get(2)).isEqualTo(unit4);
      assertThat(sortedUnits.get(3)).isEqualTo(unit1);
      assertThat(sortedUnits.get(4)).isEqualTo(unit2);
    }
  }

  @Nested
  class GetMaxAaAttackAndDiceSides {

    @Test
    void singleUnitWithCustomDice() {
      final GameData gameData = givenGameData().build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(8);

      final AaPowerStrengthAndRolls aaPowerAndRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              1,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(aaPowerAndRolls.getDiceSides())
          .as("Dice comes from the unitAttachment")
          .isEqualTo(8);
    }

    @Test
    void singleDefensiveUnitWithCustomDice() {
      final GameData gameData = givenGameData().build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setAttackAa(2).setMaxAaAttacks(1).setAttackAaMaxDieSides(8);

      final AaPowerStrengthAndRolls aaPowerAndRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              1,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.DEFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(aaPowerAndRolls.getDiceSides())
          .as("Dice comes from the unitAttachment")
          .isEqualTo(8);
    }

    @Test
    void singleUnitWithSupport() throws GameParseException {
      final GameData gameData = givenGameData().build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(4);

      final Unit supportUnit = givenUnit("support", player);
      final UnitSupportAttachment unitSupportAttachment =
          new UnitSupportAttachment("rule", supportUnit.getType(), gameData)
              .setBonus(2)
              .setBonusType("bonus")
              .setDice("AAstrength:AAroll")
              .setNumber(1)
              .setPlayers(List.of(player))
              .setSide("offence")
              .setFaction("allied")
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit),
                  List.of(unitSupportAttachment),
                  BattleState.Side.OFFENSE,
                  true));

      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              1,
              AaOffenseCombatValue.builder()
                  .friendUnits(List.of(unit, supportUnit))
                  .enemyUnits(List.of())
                  .rollSupportFromFriends(friendlySupport)
                  .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromFriends(friendlySupport.copy())
                  .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(totalPowerAndTotalRolls.getDiceSides())
          .as("Unit has a max die side of 4 so that will be used")
          .isEqualTo(4);
      assertThat(totalPowerAndTotalRolls.getBestStrength())
          .as("Unit gets 2 support so its best strength is 4")
          .isEqualTo(4);

      // Now, test the same thing through the CombatValueBuilder.aaCombatValue() API.
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls2 =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              1,
              CombatValueBuilder.aaCombatValue()
                  .friendlyUnits(List.of(unit, supportUnit))
                  .enemyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of(unitSupportAttachment))
                  .build());
      assertThat(totalPowerAndTotalRolls2.getDiceSides())
          .as("Unit has a max die side of 4 so that will be used")
          .isEqualTo(4);
      assertThat(totalPowerAndTotalRolls2.getBestStrength())
          .as("Unit gets 2 support so its best strength is 4")
          .isEqualTo(4);
    }

    @Test
    void singleUnitWithAARollSupport() throws GameParseException {
      final GameData gameData = givenGameData().build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(4);

      final Unit supportUnit = givenUnit("support", player);
      final UnitSupportAttachment unitSupportAttachment =
          new UnitSupportAttachment("rule", supportUnit.getType(), gameData)
              .setBonus(2)
              .setBonusType("bonus")
              .setDice("AAroll")
              .setNumber(1)
              .setPlayers(List.of(player))
              .setSide("offence")
              .setFaction("allied")
              .setUnitType(Set.of(unit.getType()));

      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              3,
              CombatValueBuilder.aaCombatValue()
                  .friendlyUnits(List.of(unit, supportUnit))
                  .enemyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of(unitSupportAttachment))
                  .build());

      assertThat(totalPowerAndTotalRolls.getDiceSides())
          .as("Unit has a max die side of 4 so that will be used")
          .isEqualTo(4);
      assertThat(totalPowerAndTotalRolls.getRolls(unit))
          .as("Unit should get 3 rolls via a bonus of 2")
          .isEqualTo(3);
      assertThat(totalPowerAndTotalRolls.getBestStrength())
          .as("Unit gets no strength support so its best strength is 2")
          .isEqualTo(2);
    }

    @Test
    void multipleUnitsWithSameDice() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);
      final Unit unit3 = givenUnit("test3", player);
      unit3.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(1);

      final AaPowerStrengthAndRolls aaPowerAndRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2, unit3),
              1,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(aaPowerAndRolls.getBestStrength())
          .as("All have the same dice sides, so take the best strength")
          .isEqualTo(4);
    }

    @Test
    void multipleUnitsWithDifferentDice() {
      final GameData gameData = givenGameData().build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(6);
      final Unit unit2 = givenUnit("test2", player);
      unit2
          .getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(5);
      final Unit unit3 = givenUnit("test3", player);
      unit3
          .getUnitAttachment()
          .setOffensiveAttackAa(4)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(4);

      final AaPowerStrengthAndRolls aaPowerAndRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2, unit3),
              1,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(aaPowerAndRolls.getBestStrength())
          .as("4 of 4 is better than 2 of 6 and 3 of 5")
          .isEqualTo(4);
      assertThat(aaPowerAndRolls.getDiceSides())
          .as("4 of 4 is better than 2 of 6 and 3 of 5")
          .isEqualTo(4);
    }

    @Test
    void multipleUnitsWithDifferentDice2() {
      final GameData gameData = givenGameData().build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(8);
      final Unit unit2 = givenUnit("test2", player);
      unit2
          .getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(7);
      final Unit unit3 = givenUnit("test3", player);
      unit3
          .getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(6);

      final AaPowerStrengthAndRolls aaPowerAndRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2, unit3),
              1,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(aaPowerAndRolls.getBestStrength())
          .as("3 of 6 is better than 3 of 7 and 3 of 8")
          .isEqualTo(3);
      assertThat(aaPowerAndRolls.getDiceSides())
          .as("3 of 6 is better than 3 of 7 and 3 of 8")
          .isEqualTo(6);
    }
  }

  @Nested
  class GetAaUnitPowerAndRollsForNormalBattles {

    @Test
    void unitWithZeroRollsAlwaysGetsZeroStrength() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(0);

      final AaPowerStrengthAndRolls result =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              1,
              AaOffenseCombatValue.builder()
                  .rollSupportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(result.getStrength(unit)).isEqualTo(0);
    }

    @Test
    void unitWithZeroPowerAlwaysGetsZeroRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(0).setMaxAaAttacks(1);

      final AaPowerStrengthAndRolls result =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              1,
              AaOffenseCombatValue.builder()
                  .rollSupportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(result.getRolls(unit)).isEqualTo(0);
    }

    @Test
    void strongestAaGetsSupport() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit strongUnit = givenUnit("strong", player);
      strongUnit.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(1);
      final Unit weakUnit = givenUnit("weak", player);
      weakUnit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit lessWeakUnit = givenUnit("lessWeak", player);
      lessWeakUnit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", player);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(player, supportUnit.getType(), "test", "AAstrength:AAroll")
              .setBonus(1)
              .setUnitType(
                  Set.of(strongUnit.getType(), weakUnit.getType(), lessWeakUnit.getType()));

      final AvailableSupports rollSupportFromFriends =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit),
                  List.of(unitSupportAttachment),
                  BattleState.Side.OFFENSE,
                  true));
      final AvailableSupports strengthSupportFromFriends =
          rollSupportFromFriends.filter(UnitSupportAttachment::getAaStrength);

      final AaPowerStrengthAndRolls result =
          AaPowerStrengthAndRolls.build(
              List.of(weakUnit, strongUnit, lessWeakUnit),
              4,
              AaOffenseCombatValue.builder()
                  .rollSupportFromFriends(rollSupportFromFriends)
                  .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromFriends(strengthSupportFromFriends)
                  .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(result.getStrength(strongUnit))
          .as("The strong unit should get the bonus for its power")
          .isEqualTo(5);
      assertThat(result.getRolls(strongUnit))
          .as("The strong unit should get the bonus for its rolls")
          .isEqualTo(2);
      assertThat(result.getStrength(lessWeakUnit))
          .as("The less weak unit should get no bonus")
          .isEqualTo(3);
      assertThat(result.getRolls(lessWeakUnit))
          .as("The less weak unit should get no bonus")
          .isEqualTo(1);
      assertThat(result.getStrength(weakUnit)).as("The weak unit should get no bonus").isEqualTo(2);
      assertThat(result.getRolls(weakUnit)).as("The weak unit should get no bonus").isEqualTo(1);
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class GetUnitPowerAndRollsForNormalBattles {

    @Test
    void unitWithZeroRollsAlwaysGetsZeroPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setAttack(1).setAttackRolls(0);

      final PowerStrengthAndRolls result =
          PowerStrengthAndRolls.build(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameSequence(gameData.getSequence())
                  .gameDiceSides(6)
                  .lhtrHeavyBombers(false)
                  .rollSupportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build());

      assertThat(result.getStrength(unit)).isEqualTo(0);
    }

    @Test
    void unitWithZeroPowerAlwaysGetsZeroRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setAttack(0).setAttackRolls(1);

      final PowerStrengthAndRolls result =
          PowerStrengthAndRolls.build(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameSequence(gameData.getSequence())
                  .gameDiceSides(6)
                  .lhtrHeavyBombers(false)
                  .rollSupportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build());
      assertThat(result.getRolls(unit)).isEqualTo(0);
    }

    @Test
    void attackUnitsWithMultipleSupportUnits() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType, player);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);
      final Unit otherSupportedUnit = givenUnit(unitType, player);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);
      final Unit nonSupportedUnit = givenUnit(unitType, player);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Unit supportUnit = givenUnit("support", player);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(player, supportUnit.getType(), "test", "strength:roll")
              .setNumber(2)
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));
      final Unit supportUnit2 = givenUnit("support2", player);
      final UnitSupportAttachment unitSupportAttachment2 =
          givenUnitSupportAttachment(player, supportUnit2.getType(), "test2", "strength:roll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports rollSupportFromFriends =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit, supportUnit2),
                  List.of(unitSupportAttachment, unitSupportAttachment2),
                  BattleState.Side.OFFENSE,
                  true));
      final AvailableSupports strengthSupportFromFriends =
          rollSupportFromFriends.filter(UnitSupportAttachment::getStrength);

      final PowerStrengthAndRolls result =
          PowerStrengthAndRolls.build(
              List.of(unit, otherSupportedUnit, nonSupportedUnit),
              MainOffenseCombatValue.builder()
                  .gameSequence(gameData.getSequence())
                  .gameDiceSides(6)
                  .lhtrHeavyBombers(false)
                  .rollSupportFromFriends(rollSupportFromFriends)
                  .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .strengthSupportFromFriends(strengthSupportFromFriends)
                  .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build());

      assertThat(result.getStrength(unit)).as("First should have both support").isEqualTo(3);
      assertThat(result.getRolls(unit)).as("First should have both support").isEqualTo(3);
      assertThat(result.getStrength(otherSupportedUnit))
          .as("Second should have one support")
          .isEqualTo(2);
      assertThat(result.getRolls(otherSupportedUnit))
          .as("Second should have one support")
          .isEqualTo(2);
      assertThat(result.getStrength(nonSupportedUnit))
          .as("Last should have no support")
          .isEqualTo(1);
      assertThat(result.getRolls(nonSupportedUnit)).as("Last should have no support").isEqualTo(1);

      assertThat(result.getUnitSupportPowerMap())
          .as("First support unit supported two, the second supported one")
          .isEqualTo(
              Map.of(
                  supportUnit,
                  new IntegerMap<>(Map.of(unit, 1, otherSupportedUnit, 1)),
                  supportUnit2,
                  new IntegerMap<>(Map.of(unit, 1))));
      assertThat(result.getUnitSupportRollsMap())
          .as("First support unit supported two, the second supported one")
          .isEqualTo(
              Map.of(
                  supportUnit, new IntegerMap<>(Map.of(unit, 1, otherSupportedUnit, 1)),
                  supportUnit2, new IntegerMap<>(Map.of(unit, 1))));
    }
  }

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @Nested
  class GetTotalPowerAndRolls {

    @Test
    @DisplayName("If either power or rolls is 0, then don't add the other value if it is not 0")
    void noPowerOrRollsIsZeroTotalPowerAndRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(0).setMaxAaAttacks(10);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(10).setMaxAaAttacks(0);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit, unit2),
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(powerStrengthAndRolls.calculateTotalPower()).isEqualTo(0);
      assertThat(powerStrengthAndRolls.calculateTotalRolls()).isEqualTo(0);
    }

    @Test
    void rollOfOneJustAddsPowerAndRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit, unit2),
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(powerStrengthAndRolls.calculateTotalPower()).isEqualTo(5);
      assertThat(powerStrengthAndRolls.calculateTotalRolls()).isEqualTo(2);
    }

    @Test
    void rollIsMultipliedWithPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit, unit2),
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(powerStrengthAndRolls.calculateTotalPower()).isEqualTo(10);
      assertThat(powerStrengthAndRolls.calculateTotalRolls()).isEqualTo(4);
    }

    @Test
    @DisplayName("If the power is more than the dice sides, then dice sides will be used")
    void individualPowerIsLimitedToDiceSides() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(8).setMaxAaAttacks(2);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit),
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(powerStrengthAndRolls.calculateTotalPower()).isEqualTo(12);
      assertThat(powerStrengthAndRolls.calculateTotalRolls()).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("bestRollSimulated")
    void lhtrIsSimulatedWithALittleExtraPower(
        final int strength,
        final int rolls,
        final int diceSides,
        final int expectedPower,
        final int expectedRolls) {
      final GameData gameData = givenGameData().withDiceSides(diceSides).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setAttack(strength).setAttackRolls(rolls);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit),
              CombatValueBuilder.mainCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .gameSequence(gameData.getSequence())
                  .supportAttachments(gameData.getUnitTypeList().getSupportRules())
                  .lhtrHeavyBombers(true)
                  .gameDiceSides(diceSides)
                  .territoryEffects(List.of())
                  .build());

      assertThat(powerStrengthAndRolls.calculateTotalPower()).isEqualTo(expectedPower);
      assertThat(powerStrengthAndRolls.calculateTotalRolls()).isEqualTo(expectedRolls);
    }

    List<Arguments> bestRollSimulated() {
      // expectedPower = power + ((6 / dice) * (rolls - 1))
      return List.of(
          Arguments.of(3, 2, 6, 4, 2), // 3 + (6/6) * (2-1) = 4
          Arguments.of(4, 2, 6, 5, 2), // 4 + (6/6) * (2-1) = 5
          Arguments.of(3, 3, 6, 5, 3), // 3 + (6/6) * (3-1) = 5
          Arguments.of(3, 2, 12, 5, 2), // 3 + (12/6) * (2-1) = 5
          Arguments.of(3, 3, 12, 7, 3) // 3 + (12/6) * (3-1) = 7
          );
    }

    @ParameterizedTest
    @MethodSource("bestRollSimulated")
    void chooseBestRollIsSimulatedWithALittleExtraPower(
        final int strength,
        final int rolls,
        final int diceSides,
        final int expectedPower,
        final int expectedRolls) {
      final GameData gameData = givenGameData().withDiceSides(diceSides).build();

      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setAttack(strength).setAttackRolls(rolls).setChooseBestRoll(true);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit),
              CombatValueBuilder.mainCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .gameSequence(gameData.getSequence())
                  .supportAttachments(gameData.getUnitTypeList().getSupportRules())
                  .lhtrHeavyBombers(false)
                  .gameDiceSides(diceSides)
                  .territoryEffects(List.of())
                  .build());

      assertThat(powerStrengthAndRolls.calculateTotalPower()).isEqualTo(expectedPower);
      assertThat(powerStrengthAndRolls.calculateTotalRolls()).isEqualTo(expectedRolls);
    }
  }

  @Nested
  class GetTotalAaRolls {

    @Test
    void noTargetsIsZeroRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(0).setMaxAaAttacks(0);

      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              0,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(totalPowerAndTotalRolls.calculateTotalRolls())
          .as("No targets so no rolls")
          .isEqualTo(0);
    }

    @Test
    void zeroStrengthOrZeroRollIsZeroTotalRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(0).setMaxAaAttacks(10);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(10).setMaxAaAttacks(0);

      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2),
              1,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(totalPowerAndTotalRolls.calculateTotalRolls())
          .as("Both units had either zero rolls or zero strength so no total rolls")
          .isEqualTo(0);
    }

    @Test
    void unitWithInfiniteRollsMeansRollsEqualToTarget() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              3,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());
      assertThat(totalPowerAndTotalRolls.calculateTotalRolls())
          .as("Infinite unit gets one roll for each target")
          .isEqualTo(3);
    }

    @Test
    void multipleUnitsWithInfiniteRollsMeansRollsEqualToTarget() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2),
              3,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());
      assertThat(totalPowerAndTotalRolls.calculateTotalRolls())
          .as("Infinite unit gets one roll for each target but no overstacking")
          .isEqualTo(3);
    }

    @Test
    void infiniteUnitAndNonInfiniteUnitMeansRollsEqualsToTarget() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(10);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2),
              3,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());
      assertThat(totalPowerAndTotalRolls.calculateTotalRolls())
          .as("Non infinite and an infinite unit still just hit all the targets once")
          .isEqualTo(3);
    }

    @Test
    void rollsOfNonInfiniteUnitEqualsRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", givenPlayer(gameData));
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              3,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());
      assertThat(totalPowerAndTotalRolls.calculateTotalRolls())
          .as("Unit only has one roll")
          .isEqualTo(1);
    }

    @Test
    void rollsOfNonInfiniteUnitGreaterThanTargetCountMeansRollsEqualsTarget() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit unit = givenUnit("test", player);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(2);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2),
              3,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());
      assertThat(totalPowerAndTotalRolls.calculateTotalRolls())
          .as("There is only 3 units targets and the units have no overstack so only allow 3")
          .isEqualTo(3);
    }

    @Test
    void overstackUnitCanCauseRollsToGoOverTargetCount() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final GamePlayer player = givenPlayer(gameData);
      final Unit overstackUnit = givenUnit("test", player);
      overstackUnit
          .getUnitAttachment()
          .setOffensiveAttackAa(1)
          .setMaxAaAttacks(2)
          .setMayOverStackAa(true);
      final Unit unit = givenUnit("test2", player);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(3);
      final Unit unit2 = givenUnit("test3", player);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(overstackUnit, unit, unit2),
              3,
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.OFFENSE)
                  .supportAttachments(List.of())
                  .build());

      assertThat(totalPowerAndTotalRolls.calculateTotalRolls())
          .as(
              "Infinite gives total attacks equal to number of units (3)"
                  + " and the overstacked unit adds 2 more")
          .isEqualTo(5);
    }
  }
}
