package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Die;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.collections.IntegerMap;

@ExtendWith(MockitoExtension.class)
class TotalPowerAndTotalRollsTest {

  @Mock GamePlayer owner;

  private Unit givenUnit(final String name, final GameData gameData) {
    return givenUnit(givenUnitType(name, gameData));
  }

  private Unit givenUnit(final UnitType unitType) {
    return unitType.create(1, owner, true).get(0);
  }

  private UnitType givenUnitType(final String name, final GameData gameData) {
    final UnitType unitType = new UnitType(name + "Type", gameData);
    final UnitAttachment unitAttachment =
        new UnitAttachment(name + "Attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    return unitType;
  }

  UnitSupportAttachment givenUnitSupportAttachment(
      final GameData gameData, final UnitType unitType, final String name, final String diceType)
      throws GameParseException {
    return new UnitSupportAttachment("rule" + name, unitType, gameData)
        .setBonus(1)
        .setBonusType("bonus" + name)
        .setDice(diceType)
        .setNumber(1)
        .setPlayers(List.of(owner))
        .setSide("offence")
        .setFaction("allied");
  }

  @Nested
  class GetTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack {

    @Test
    void singleAaWithOneRoll() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "Total Power equals the single unit's strength", result.calculateTotalPower(), is(2));
      assertThat("Only one unit, so only one strength", result.isSameStrength(), is(true));
      assertThat(sortedDie, is(List.of(new Die(1, 2, Die.DieType.HIT))));
    }

    private AaPowerStrengthAndRolls whenGetPowerHitsResult(
        final GameData gameData,
        final List<Unit> units,
        final List<Die> sortedDie,
        final int dieHit,
        final int numValidTargets) {
      final AaPowerStrengthAndRolls unitPowerAndRollsMap =
          AaPowerStrengthAndRolls.build(
              units,
              numValidTargets,
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      final int totalRolls = unitPowerAndRollsMap.calculateTotalRolls();
      final int[] dice = new int[totalRolls];
      for (int i = 0; i < totalRolls; i++) {
        dice[i] = dieHit;
      }

      sortedDie.addAll(unitPowerAndRollsMap.getDiceHits(dice));
      return unitPowerAndRollsMap;
    }

    @Test
    void singleAaWithOneRollNoHit() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      whenGetPowerHitsResult(gameData, units, sortedDie, 6, 4);

      assertThat(
          "The strength was 2 but the dice rolled a 6 so it was a miss",
          sortedDie,
          is(List.of(new Die(6, 2, Die.DieType.MISS))));
    }

    @Test
    void singleAaWithTwoRoll() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "2 strength in 2 rolls equals total power of 4", result.calculateTotalPower(), is(4));
      assertThat("Only one unit, so only one strength", result.isSameStrength(), is(true));
      assertThat(
          sortedDie, is(List.of(new Die(1, 2, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void singleAaWithMoreRollsThanTargets() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(3);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 2);

      assertThat(
          "Unit has 3 rolls but only 2 targets, so 2 rolls of 2 strength = 4",
          result.calculateTotalPower(),
          is(4));
      assertThat("Only one unit, so only one strength", result.isSameStrength(), is(true));
      assertThat(
          sortedDie, is(List.of(new Die(1, 2, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void twoAaWithSamePower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat("2 strength + 2 strength is 4", result.calculateTotalPower(), is(4));
      assertThat("Both units have the same strength", result.isSameStrength(), is(true));
      assertThat(
          sortedDie, is(List.of(new Die(1, 2, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void twoAaWithDifferentPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);

      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat("2 strength + 3 strength is 5", result.calculateTotalPower(), is(5));
      assertThat("Both units have different strength values", result.isSameStrength(), is(false));
      assertThat(
          sortedDie, is(List.of(new Die(1, 3, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void twoAaWithDifferentPowerAndMoreRollsThanTargets() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2);

      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 3);

      assertThat(
          "The second unit has higher strength so it rolls both "
              + "and the first unit only rolls once. 3 * 2 + 2",
          result.calculateTotalPower(),
          is(8));
      assertThat(
          "First two dice are from the second stronger unit",
          sortedDie,
          is(
              List.of(
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void twoAaWithDifferentPowerAndOnlyOneHit() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      whenGetPowerHitsResult(gameData, units, sortedDie, 2, 4);

      assertThat(
          "The dice is a 2 so the first unit hits (with a strength of 3) "
              + "but the second misses (with a strength of 2). "
              + "Strength is 1 based and the dice value is 0 based.",
          sortedDie,
          is(List.of(new Die(2, 3, Die.DieType.HIT), new Die(2, 2, Die.DieType.MISS))));
    }

    @Test
    void oneAaWithInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "Infinite strength of 2 is multiplied by the rolls so 8",
          result.calculateTotalPower(),
          is(8));
      assertThat("Only one unit, so only one strength", result.isSameStrength(), is(true));
      assertThat(
          sortedDie,
          is(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void twoAaWithInfiniteWithSamePower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "Two infinite units are equal to one infinite unit", result.calculateTotalPower(), is(8));
      assertThat(
          "Only one infinite unit is used and it always has the same strength",
          result.isSameStrength(),
          is(true));
      assertThat(
          sortedDie,
          is(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void twoAaWithInfiniteWithDifferentPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "The strongest infinite unit is used for all targets",
          result.calculateTotalPower(),
          is(12));
      assertThat(
          "Only one infinite unit and it always has the same strength",
          result.isSameStrength(),
          is(true));
      assertThat(
          sortedDie,
          is(
              List.of(
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT))));
    }

    @Test
    void twoAaWithInfiniteWithDifferentDice() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(-1)
          .setOffensiveAttackAaMaxDieSides(4);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2
          .getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(-1)
          .setOffensiveAttackAaMaxDieSides(8);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "2 of 4 is better than 3 of 8 so the 2 strength is used for all targets",
          result.calculateTotalPower(),
          is(8));
      assertThat(
          "Only one infinite unit and it always has the same strength",
          result.isSameStrength(),
          is(true));
      assertThat(
          "2 of 4 is better than 3 of 8 so that is used for strength and dice sides",
          sortedDie,
          is(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void twoAaWithOneRollAndInfiniteSamePower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "Both units have strength 2 so the power is 2 * 4 (rolls) = 8",
          result.calculateTotalPower(),
          is(8));
      assertThat("Both units have the same strength", result.isSameStrength(), is(true));
      assertThat(
          sortedDie,
          is(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void twoAaWithOneRollAndInfiniteWhereInfiniteIsHigher() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "The non infinite unit is not used so the power is 3 (strength) * 4 (roll)",
          result.calculateTotalPower(),
          is(12));
      assertThat(
          "The non infinite unit is not used so the infinite unit always has the same strength",
          result.isSameStrength(),
          is(true));
      assertThat(
          "The non infinite unit is not used",
          sortedDie,
          is(
              List.of(
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT))));
    }

    @Test
    void twoAaWithOneRollAndInfiniteWhereInfiniteIsLower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "The non infinite unit is used once so 3 + 2 * 3", result.calculateTotalPower(), is(9));
      assertThat(
          "The non infinite has a higher strength than the infinite so both are used",
          result.isSameStrength(),
          is(false));
      assertThat(
          "The non infinite unit is used first",
          sortedDie,
          is(
              List.of(
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void oneAaWithOverStack() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat("2 rolls with 2 strength is 4 power", result.calculateTotalPower(), is(4));
      assertThat("Only one unit, so only one strength", result.isSameStrength(), is(true));
      assertThat(
          sortedDie, is(List.of(new Die(1, 2, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void oneAaWithOverStackAndMoreRollsThanTargets() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(5).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "Unit has 5 rolls with 2 strength and can overstack, so 5 * 2",
          result.calculateTotalPower(),
          is(10));
      assertThat("Only one unit, so only one strength", result.isSameStrength(), is(true));
      assertThat(
          sortedDie,
          is(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void oneAaWithOverstackAndInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 2);

      assertThat(
          "Overstack makes no sense on an infinite unit. "
              + "Unit gets 1 roll for each target: 2 (roll) * 2 (strength)",
          result.calculateTotalPower(),
          is(4));
      assertThat(result.isSameStrength(), is(true));
      assertThat(
          sortedDie, is(List.of(new Die(1, 2, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void oneOverstackAndOneInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "Infinite unit hits all 4, overstack unit adds 2 more: 6 (roll) * 2 (strength)",
          result.calculateTotalPower(),
          is(12));
      assertThat("Both units have the same strength", result.isSameStrength(), is(true));
      assertThat(
          "Infiniteunit hits all 4, overstack unit adds 2 more",
          sortedDie,
          is(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void oneOverstackAndOneInfiniteDifferentPowers() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(
          "Infinite unit hits all 4 with strength 2, "
              + "overstack unit adds 2 more with strength 3: 4 * 2 + 3 * 2",
          result.calculateTotalPower(),
          is(14));
      assertThat("Both units have different strength", result.isSameStrength(), is(false));
      assertThat(
          "Infinite unit hits all 4, overstack unit adds 2 more at the end",
          sortedDie,
          is(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT))));
    }

    @Test
    void oneOverstackAndOneNormal() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower(), is(8));
      assertThat(result.isSameStrength(), is(true));
      assertThat(
          "Overstack adds more rolls",
          sortedDie,
          is(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void oneOverstackAndOneNormalDifferentPowers() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower(), is(10));
      assertThat(result.isSameStrength(), is(false));
      assertThat(
          "Overstack adds more rolls at the end",
          sortedDie,
          is(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT))));
    }

    @Test
    void oneOverstackOneInfiniteAndOneNormalSamePower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2, unit3);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower(), is(12));
      assertThat(result.isSameStrength(), is(true));
      assertThat(
          "Overstack adds more rolls",
          sortedDie,
          is(
              List.of(
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void oneOverstackOneInfiniteAndOneNormalDifferentPowersWhereNormalIsBest() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2, unit3);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower(), is(18));
      assertThat(result.isSameStrength(), is(false));
      assertThat(
          "Overstack adds more rolls",
          sortedDie,
          is(
              List.of(
                  new Die(1, 4, Die.DieType.HIT),
                  new Die(1, 4, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(1, 2, Die.DieType.HIT))));
    }

    @Test
    void oneOverstackOneInfiniteAndOneNormalDifferentPowersWhereNormalIsWorst() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2, unit3);
      final List<Die> sortedDie = new ArrayList<>();

      final AaPowerStrengthAndRolls result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result.calculateTotalPower(), is(20));
      assertThat(result.isSameStrength(), is(false));
      assertThat(
          "Overstack adds more rolls",
          sortedDie,
          is(
              List.of(
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(1, 4, Die.DieType.HIT),
                  new Die(1, 4, Die.DieType.HIT))));
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
      unit1 = givenUnit("test1", gameData);
      unit2 = givenUnit("test2", gameData);
      unit3 = givenUnit("test3", gameData);
      unit4 = givenUnit("test4", gameData);
      unit5 = givenUnit("test5", gameData);
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
                  AaPowerStrengthAndRolls.sortAaHighToLow(
                      CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData)))
              .collect(Collectors.toList());
      assertThat(sortedUnits.get(0), is(unit1));
      assertThat(sortedUnits.get(1), is(unit2));
      assertThat(sortedUnits.get(2), is(unit3));
      assertThat(sortedUnits.get(3), is(unit4));
      assertThat(sortedUnits.get(4), is(unit5));
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
                  AaPowerStrengthAndRolls.sortAaHighToLow(
                      CombatValue.buildAaCombatValue(List.of(), List.of(), true, gameData)))
              .collect(Collectors.toList());
      assertThat(sortedUnits.get(0), is(unit5));
      assertThat(sortedUnits.get(1), is(unit3));
      assertThat(sortedUnits.get(2), is(unit4));
      assertThat(sortedUnits.get(3), is(unit1));
      assertThat(sortedUnits.get(4), is(unit2));
    }
  }

  @Nested
  class GetMaxAaAttackAndDiceSides {

    @Test
    void singleUnitWithCustomDice() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(8);

      final AaPowerStrengthAndRolls aaPowerAndRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              1,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat("Dice comes from the unitAttachment", aaPowerAndRolls.getBestDiceSides(), is(8));
    }

    @Test
    void singleDefensiveUnitWithCustomDice() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttackAa(2).setMaxAaAttacks(1).setAttackAaMaxDieSides(8);

      final AaPowerStrengthAndRolls aaPowerAndRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              1,
              CombatValue.buildAaCombatValue(List.of(), List.of(), true, gameData));

      assertThat("Dice comes from the unitAttachment", aaPowerAndRolls.getBestDiceSides(), is(8));
    }

    @Test
    void singleUnitWithSupport() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);

      final CombatValue combatValue = mock(CombatValue.class);
      final RollCalculator rollCalculator = mock(RollCalculator.class);
      when(rollCalculator.getRoll(unit)).thenReturn(RollValue.of(2));
      when(combatValue.getRoll()).thenReturn(rollCalculator);
      final StrengthCalculator strengthCalculator = mock(StrengthCalculator.class);
      when(strengthCalculator.getStrength(unit)).thenReturn(StrengthValue.of(6, 3));
      when(combatValue.getStrength()).thenReturn(strengthCalculator);
      final PowerCalculator powerCalculator =
          new PowerCalculator(
              gameData, strengthCalculator, rollCalculator, (unit1) -> true, (unit1) -> 6);
      when(combatValue.getPower()).thenReturn(powerCalculator);
      when(combatValue.getDiceSides(unit)).thenReturn(6);
      when(combatValue.getGameData()).thenReturn(gameData);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(List.of(unit), 1, combatValue);

      assertThat("Calculated value is used", totalPowerAndTotalRolls.getBestDiceSides(), is(6));
      assertThat("Calculated value is used", totalPowerAndTotalRolls.getBestStrength(), is(3));
    }

    @Test
    void multipleUnitsWithSameDice() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(1);

      final AaPowerStrengthAndRolls aaPowerAndRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2, unit3),
              1,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat(
          "All have the same dice sides, so take the best strength",
          aaPowerAndRolls.getBestStrength(),
          is(4));
    }

    @Test
    void multipleUnitsWithDifferentDice() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(6);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2
          .getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(5);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3
          .getUnitAttachment()
          .setOffensiveAttackAa(4)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(4);

      final AaPowerStrengthAndRolls aaPowerAndRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2, unit3),
              1,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat(
          "4 of 4 is better than 2 of 6 and 3 of 5", aaPowerAndRolls.getBestStrength(), is(4));
      assertThat(
          "4 of 4 is better than 2 of 6 and 3 of 5", aaPowerAndRolls.getBestDiceSides(), is(4));
    }

    @Test
    void multipleUnitsWithDifferentDice2() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(8);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2
          .getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(7);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3
          .getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(6);

      final AaPowerStrengthAndRolls aaPowerAndRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2, unit3),
              1,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat(
          "3 of 6 is better than 3 of 7 and 3 of 8", aaPowerAndRolls.getBestStrength(), is(3));
      assertThat(
          "3 of 6 is better than 3 of 7 and 3 of 8", aaPowerAndRolls.getBestDiceSides(), is(6));
    }
  }

  @Nested
  class GetAaUnitPowerAndRollsForNormalBattles {

    @Test
    void unitWithZeroRollsAlwaysGetsZeroStrength() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(0);

      final AaPowerStrengthAndRolls result =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              1,
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(result.getStrength(unit), is(0));
    }

    @Test
    void unitWithZeroPowerAlwaysGetsZeroRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(0).setMaxAaAttacks(1);

      final AaPowerStrengthAndRolls result =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              1,
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(result.getRolls(unit), is(0));
    }

    @Test
    void strongestAaGetsSupport() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit strongUnit = givenUnit("strong", gameData);
      strongUnit.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(1);
      final Unit weakUnit = givenUnit("weak", gameData);
      weakUnit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit lessWeakUnit = givenUnit("lessWeak", gameData);
      lessWeakUnit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAstrength:AAroll")
              .setBonus(1)
              .setUnitType(
                  Set.of(strongUnit.getType(), weakUnit.getType(), lessWeakUnit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final AaPowerStrengthAndRolls result =
          AaPowerStrengthAndRolls.build(
              List.of(weakUnit, strongUnit, lessWeakUnit),
              4,
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          "The strong unit should get the bonus for its power",
          result.getStrength(strongUnit),
          is(5));
      assertThat(
          "The strong unit should get the bonus for its rolls", result.getRolls(strongUnit), is(2));
      assertThat("The less weak unit should get no bonus", result.getStrength(lessWeakUnit), is(3));
      assertThat("The less weak unit should get no bonus", result.getRolls(lessWeakUnit), is(1));
      assertThat("The weak unit should get no bonus", result.getStrength(weakUnit), is(2));
      assertThat("The weak unit should get no bonus", result.getRolls(weakUnit), is(1));
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class GetUnitPowerAndRollsForNormalBattles {

    @Test
    void unitWithZeroRollsAlwaysGetsZeroPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(0);

      final PowerStrengthAndRolls result =
          PowerStrengthAndRolls.build(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build());

      assertThat(result.getStrength(unit), is(0));
    }

    @Test
    void unitWithZeroPowerAlwaysGetsZeroRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(0).setAttackRolls(1);

      final PowerStrengthAndRolls result =
          PowerStrengthAndRolls.build(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build());
      assertThat(result.getRolls(unit), is(0));
    }

    @Test
    void attackUnitsWithMultipleSupportUnits() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);
      final Unit otherSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);
      final Unit nonSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength:roll")
              .setNumber(2)
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));
      final Unit supportUnit2 = givenUnit("support2", gameData);
      final UnitSupportAttachment unitSupportAttachment2 =
          givenUnitSupportAttachment(gameData, supportUnit2.getType(), "test2", "strength:roll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit, supportUnit2),
                  List.of(unitSupportAttachment, unitSupportAttachment2),
                  false,
                  true));

      final PowerStrengthAndRolls result =
          PowerStrengthAndRolls.build(
              List.of(unit, otherSupportedUnit, nonSupportedUnit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build());

      assertThat("First should have both support", result.getStrength(unit), is(3));
      assertThat("First should have both support", result.getRolls(unit), is(3));
      assertThat("second should have one support", result.getStrength(otherSupportedUnit), is(2));
      assertThat("second should have one support", result.getRolls(otherSupportedUnit), is(2));
      assertThat("last should have no support", result.getStrength(nonSupportedUnit), is(1));
      assertThat("last should have no support", result.getRolls(nonSupportedUnit), is(1));

      assertThat(
          "First support unit supported two, the second supported one",
          result.getUnitSupportPowerMap(),
          is(
              Map.of(
                  supportUnit,
                  new IntegerMap<>(Map.of(unit, 1, otherSupportedUnit, 1)),
                  supportUnit2,
                  new IntegerMap<>(Map.of(unit, 1)))));
      assertThat(
          "First support unit supported two, the second supported one",
          result.getUnitSupportRollsMap(),
          is(
              Map.of(
                  supportUnit, new IntegerMap<>(Map.of(unit, 1, otherSupportedUnit, 1)),
                  supportUnit2, new IntegerMap<>(Map.of(unit, 1)))));
    }
  }

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @Nested
  class GetTotalPowerAndRolls {

    @Test
    @DisplayName("If either power or rolls is 0, then don't add the other value if it is not 0")
    void noPowerOrRollsIsZeroTotalPowerAndRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(0).setMaxAaAttacks(10);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(10).setMaxAaAttacks(0);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit, unit2),
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat(powerStrengthAndRolls.calculateTotalPower(), is(0));
      assertThat(powerStrengthAndRolls.calculateTotalRolls(), is(0));
    }

    @Test
    void rollOfOneJustAddsPowerAndRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit, unit2),
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat(powerStrengthAndRolls.calculateTotalPower(), is(5));
      assertThat(powerStrengthAndRolls.calculateTotalRolls(), is(2));
    }

    @Test
    void rollIsMultipliedWithPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit, unit2),
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat(powerStrengthAndRolls.calculateTotalPower(), is(10));
      assertThat(powerStrengthAndRolls.calculateTotalRolls(), is(4));
    }

    @Test
    @DisplayName("If the power is more than the dice sides, then dice sides will be used")
    void individualPowerIsLimitedToDiceSides() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(8).setMaxAaAttacks(2);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit), CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat(powerStrengthAndRolls.calculateTotalPower(), is(12));
      assertThat(powerStrengthAndRolls.calculateTotalRolls(), is(2));
    }

    @ParameterizedTest
    @MethodSource("bestRollSimulated")
    void lhtrIsSimulatedWithALittleExtraPower(
        final int strength,
        final int rolls,
        final int diceSides,
        final int expectedPower,
        final int expectedRolls) {
      final GameData gameData =
          givenGameData().withDiceSides(diceSides).withLhtrHeavyBombers(true).build();

      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(strength).setAttackRolls(rolls);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit), CombatValue.buildNoSupportCombatValue(false, gameData, List.of()));

      assertThat(powerStrengthAndRolls.calculateTotalPower(), is(expectedPower));
      assertThat(powerStrengthAndRolls.calculateTotalRolls(), is(expectedRolls));
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

      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(strength).setAttackRolls(rolls).setChooseBestRoll(true);

      final PowerStrengthAndRolls powerStrengthAndRolls =
          PowerStrengthAndRolls.build(
              List.of(unit), CombatValue.buildNoSupportCombatValue(false, gameData, List.of()));

      assertThat(powerStrengthAndRolls.calculateTotalPower(), is(expectedPower));
      assertThat(powerStrengthAndRolls.calculateTotalRolls(), is(expectedRolls));
    }
  }

  @Nested
  class GetTotalAaRolls {

    @Test
    void noTargetsIsZeroRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(0).setMaxAaAttacks(0);

      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              0,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat("No targets so no rolls", totalPowerAndTotalRolls.calculateTotalRolls(), is(0));
    }

    @Test
    void zeroStrengthOrZeroRollIsZeroTotalRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(0).setMaxAaAttacks(10);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(10).setMaxAaAttacks(0);

      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2),
              1,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat(
          "Both units had either zero rolls or zero strength so no total rolls",
          totalPowerAndTotalRolls.calculateTotalRolls(),
          is(0));
    }

    @Test
    void unitWithInfiniteRollsMeansRollsEqualToTarget() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              3,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));
      assertThat(
          "Infinite unit gets one roll for each target",
          totalPowerAndTotalRolls.calculateTotalRolls(),
          is(3));
    }

    @Test
    void multipleUnitsWithInfiniteRollsMeansRollsEqualToTarget() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2),
              3,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));
      assertThat(
          "Infinite unit gets one roll for each target but no overstacking",
          totalPowerAndTotalRolls.calculateTotalRolls(),
          is(3));
    }

    @Test
    void infiniteUnitAndNonInfiniteUnitMeansRollsEqualsToTarget() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(10);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2),
              3,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));
      assertThat(
          "Non infinite and an infinite unit still just hit all the targets once",
          totalPowerAndTotalRolls.calculateTotalRolls(),
          is(3));
    }

    @Test
    void rollsOfNonInfiniteUnitEqualsRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit),
              3,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));
      assertThat("Unit only has one roll", totalPowerAndTotalRolls.calculateTotalRolls(), is(1));
    }

    @Test
    void rollsOfNonInfiniteUnitGreaterThanTargetCountMeansRollsEqualsTarget() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(2);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(unit, unit2),
              3,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));
      assertThat(
          "There is only 3 units targets and the units have no overstack so only allow 3",
          totalPowerAndTotalRolls.calculateTotalRolls(),
          is(3));
    }

    @Test
    void overstackUnitCanCauseRollsToGoOverTargetCount() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit overstackUnit = givenUnit("test", gameData);
      overstackUnit
          .getUnitAttachment()
          .setOffensiveAttackAa(1)
          .setMaxAaAttacks(2)
          .setMayOverStackAa(true);
      final Unit unit = givenUnit("test2", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(3);
      final Unit unit2 = givenUnit("test3", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final AaPowerStrengthAndRolls totalPowerAndTotalRolls =
          AaPowerStrengthAndRolls.build(
              List.of(overstackUnit, unit, unit2),
              3,
              CombatValue.buildAaCombatValue(List.of(), List.of(), false, gameData));

      assertThat(
          "Infinite gives total attacks equal to number of units (3)"
              + " and the overstacked unit adds 2 more",
          totalPowerAndTotalRolls.calculateTotalRolls(),
          is(5));
    }
  }
}
