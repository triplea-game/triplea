package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import org.triplea.util.Triple;
import org.triplea.util.Tuple;

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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(2, 1, true)));
      assertThat(sortedDie, is(List.of(new Die(1, 2, Die.DieType.HIT))));
    }

    private Triple<Integer, Integer, Boolean> whenGetPowerHitsResult(
        final GameData gameData,
        final List<Unit> units,
        final List<Die> sortedDie,
        final int dieHit,
        final int numValidTargets) {
      final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              units,
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      final List<Unit> validTargets =
          IntStream.rangeClosed(1, numValidTargets)
              .mapToObj(num -> mock(Unit.class))
              .collect(Collectors.toList());
      final int totalAttacks =
          TotalPowerAndTotalRolls.getTotalAaAttacks(unitPowerAndRollsMap, validTargets);
      final int[] dice = new int[totalAttacks];
      for (int i = 0; i < totalAttacks; i++) {
        dice[i] = dieHit;
      }

      return TotalPowerAndTotalRolls.getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(
          dice, sortedDie, false, unitPowerAndRollsMap, validTargets, gameData, true);
    }

    @Test
    void singleAaWithOneRollNoHit() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 6, 4);

      assertThat(result, is(Triple.of(2, 0, true)));
      assertThat(sortedDie, is(List.of(new Die(6, 2, Die.DieType.MISS))));
    }

    @Test
    void singleAaWithTwoRoll() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(4, 2, true)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(4, 2, true)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(5, 2, false)));
      assertThat(
          sortedDie, is(List.of(new Die(1, 3, Die.DieType.HIT), new Die(1, 2, Die.DieType.HIT))));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 2, 4);

      assertThat(result, is(Triple.of(5, 1, false)));
      assertThat(
          sortedDie, is(List.of(new Die(2, 3, Die.DieType.HIT), new Die(2, 2, Die.DieType.MISS))));
    }

    @Test
    void oneAaWithInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit);
      final List<Die> sortedDie = new ArrayList<>();

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(8, 4, true)));
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
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(8, 4, true)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(12, 4, true)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(8, 4, true)));
      assertThat(
          "2 of 4 is better than 3 of 8 so that is used for attack",
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(8, 4, true)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(12, 4, true)));
      assertThat(
          "The non infinite attack is never used",
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(9, 4, false)));
      assertThat(
          "The non infinite attack is used first",
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(4, 2, true)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(10, 5, true)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(8, 4, true)));
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
    void oneOverstackAndOneInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(12, 6, true)));
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
    void oneOverstackAndOneInfiniteDifferentPowers() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit, unit2);
      final List<Die> sortedDie = new ArrayList<>();

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(14, 6, false)));
      assertThat(
          "Overstack adds more rolls at the end",
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(8, 4, true)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(10, 4, false)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(12, 6, true)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(18, 6, false)));
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

      final Triple<Integer, Integer, Boolean> result =
          whenGetPowerHitsResult(gameData, units, sortedDie, 1, 4);

      assertThat(result, is(Triple.of(20, 6, false)));
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

    @Mock private Unit unit1;
    @Mock private Unit unit2;
    @Mock private Unit unit3;
    @Mock private Unit unit4;
    @Mock private Unit unit5;

    private final List<Unit> units = new ArrayList<>();

    private UnitAttachment setupUnitAttachment(final Unit unit) {
      final UnitType unitTypeMock = mock(UnitType.class);
      final UnitAttachment unitAttachment = mock(UnitAttachment.class);
      when(unitTypeMock.getAttachment(any())).thenReturn(unitAttachment);
      when(unit.getType()).thenReturn(unitTypeMock);
      return unitAttachment;
    }

    @BeforeEach
    void setUp() {
      units.addAll(List.of(unit1, unit2, unit3, unit4, unit5));
    }

    @Test
    void testAttacking() {
      int index = 4;
      for (final var unit : units) {
        final var unitAttachment = setupUnitAttachment(unit);
        // We're integer dividing the index at this point to get duplicate sorting keys
        // in order to reach some edge cases
        when(unitAttachment.getOffensiveAttackAa(any())).thenReturn(index / 2);
        index--;
      }
      TotalPowerAndTotalRolls.sortAaHighToLow(units, gameData, false, new HashMap<>());
      assertThat(units.get(0), is(unit1));
      assertThat(units.get(1), is(unit2));
      assertThat(units.get(2), is(unit3));
      assertThat(units.get(3), is(unit4));
      assertThat(units.get(4), is(unit5));
    }

    @Test
    void testDefending() {
      int index = 0;
      for (final var unit : units) {
        final var unitAttachment = setupUnitAttachment(unit);
        // We're integer dividing the index at this point to get duplicate sorting keys
        // in order to reach some edge cases
        when(unitAttachment.getAttackAa(any())).thenReturn(index / 2);
        index++;
      }
      TotalPowerAndTotalRolls.sortAaHighToLow(units, gameData, true, new HashMap<>());
      assertThat(units.get(0), is(unit5));
      assertThat(units.get(1), is(unit3));
      assertThat(units.get(2), is(unit4));
      assertThat(units.get(3), is(unit1));
      assertThat(units.get(4), is(unit2));
    }
  }

  @Nested
  class GetMaxAaAttackAndDiceSides {

    @Test
    void singleUnitWithNoCustomDiceAndNoPowerRollsMap() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);

      final Tuple<Integer, Integer> maxAttackAndDice =
          TotalPowerAndTotalRolls.getMaxAaAttackAndDiceSides(List.of(unit), gameData, false);

      assertThat(maxAttackAndDice, is(Tuple.of(2, 6)));
    }

    @Test
    void singleDefensiveUnitWithNoCustomDiceAndNoPowerRollsMap() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttackAa(2).setMaxAaAttacks(1);

      final Tuple<Integer, Integer> maxAttackAndDice =
          TotalPowerAndTotalRolls.getMaxAaAttackAndDiceSides(List.of(unit), gameData, true);

      assertThat(maxAttackAndDice, is(Tuple.of(2, 6)));
    }

    @Test
    void singleUnitWithCustomDiceAndNoPowerRollsMap() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(8);

      final Tuple<Integer, Integer> maxAttackAndDice =
          TotalPowerAndTotalRolls.getMaxAaAttackAndDiceSides(List.of(unit), gameData, false);

      assertThat("Dice comes from the unitAttachment", maxAttackAndDice, is(Tuple.of(2, 8)));
    }

    @Test
    void singleDefensiveUnitWithCustomDiceAndNoPowerRollsMap() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttackAa(2).setMaxAaAttacks(1).setAttackAaMaxDieSides(8);

      final Tuple<Integer, Integer> maxAttackAndDice =
          TotalPowerAndTotalRolls.getMaxAaAttackAndDiceSides(List.of(unit), gameData, true);

      assertThat("Dice comes from the unitAttachment", maxAttackAndDice, is(Tuple.of(2, 8)));
    }

    @Test
    void singleUnitWithPowerRollsMap() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);

      final Tuple<Integer, Integer> maxAttackAndDice =
          TotalPowerAndTotalRolls.getMaxAaAttackAndDiceSides(
              List.of(unit),
              gameData,
              false,
              Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(2).build()));

      assertThat(
          "Value from totalPowerAndTotalRolls is used", maxAttackAndDice, is(Tuple.of(3, 6)));
    }

    @Test
    void singleDefensiveUnitWithPowerRollsMap() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttackAa(2).setMaxAaAttacks(1);

      final Tuple<Integer, Integer> maxAttackAndDice =
          TotalPowerAndTotalRolls.getMaxAaAttackAndDiceSides(
              List.of(unit),
              gameData,
              true,
              Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(2).build()));

      assertThat(
          "Value from totalPowerAndTotalRolls is used", maxAttackAndDice, is(Tuple.of(3, 6)));
    }

    @Test
    void limitAttackToDiceSides() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(1)
          .setOffensiveAttackAaMaxDieSides(4);

      final Tuple<Integer, Integer> maxAttackAndDice =
          TotalPowerAndTotalRolls.getMaxAaAttackAndDiceSides(
              List.of(unit),
              gameData,
              false,
              Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(6).totalRolls(2).build()));

      assertThat(
          "UnitAttachment dice sides is the max allowed", maxAttackAndDice, is(Tuple.of(4, 4)));
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

      final Tuple<Integer, Integer> maxAttackAndDice =
          TotalPowerAndTotalRolls.getMaxAaAttackAndDiceSides(
              List.of(unit, unit2, unit3), gameData, false);

      assertThat(
          "UnitAttachment dice sides is the max allowed", maxAttackAndDice, is(Tuple.of(4, 6)));
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

      final Tuple<Integer, Integer> maxAttackAndDice =
          TotalPowerAndTotalRolls.getMaxAaAttackAndDiceSides(
              List.of(unit, unit2, unit3), gameData, false);

      assertThat("4 of 4 is better than 2 of 6 and 3 of 5", maxAttackAndDice, is(Tuple.of(4, 4)));
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

      final Tuple<Integer, Integer> maxAttackAndDice =
          TotalPowerAndTotalRolls.getMaxAaAttackAndDiceSides(
              List.of(unit, unit2, unit3), gameData, false);

      assertThat("3 of 6 is better than 3 of 7 and 3 of 8", maxAttackAndDice, is(Tuple.of(3, 6)));
    }
  }

  @Nested
  class GetAaUnitPowerAndRollsForNormalBattles {

    @Test
    void unitWithZeroRollsAlwaysGetsZeroPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(0);

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));
    }

    @Test
    void unitWithZeroPowerAlwaysGetsZeroRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(0).setMaxAaAttacks(1);

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));
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

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(weakUnit, strongUnit, lessWeakUnit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          "The strong unit should get the bonus for its power and rolls",
          result.get(strongUnit),
          is(TotalPowerAndTotalRolls.builder().totalPower(5).totalRolls(2).build()));
      assertThat(
          "The less weak unit should get no bonus",
          result.get(lessWeakUnit),
          is(TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(1).build()));
      assertThat(
          "The weak unit should get no bonus",
          result.get(weakUnit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(1).build()));
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

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .territoryIsLand(true)
                  .build(),
              Map.of(),
              Map.of());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));
    }

    @Test
    void unitWithZeroPowerAlwaysGetsZeroRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(0).setAttackRolls(1);

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .territoryIsLand(true)
                  .build(),
              Map.of(),
              Map.of());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));
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

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

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

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit, otherSupportedUnit, nonSupportedUnit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .territoryIsLand(true)
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          "First should have both support",
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(3).build()));
      assertThat(
          "second should have one support",
          result.get(otherSupportedUnit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build()));
      assertThat(
          "last should have no support",
          result.get(nonSupportedUnit),
          is(TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build()));

      assertThat(
          "First support unit supported two, the second supported one",
          unitSupportPowerMap,
          is(
              Map.of(
                  supportUnit,
                  new IntegerMap<>(Map.of(unit, 1, otherSupportedUnit, 1)),
                  supportUnit2,
                  new IntegerMap<>(Map.of(unit, 1)))));
      assertThat(
          "First support unit supported two, the second supported one",
          unitSupportRollsMap,
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

      assertThat(
          TotalPowerAndTotalRolls.getTotalPowerAndRolls(
              Map.of(
                  mock(Unit.class),
                  TotalPowerAndTotalRolls.builder().totalPower(10).totalRolls(0).build(),
                  mock(Unit.class),
                  TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(10).build()),
              gameData),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));
    }

    @Test
    void rollOfOneJustAddsPowerAndRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();

      assertThat(
          TotalPowerAndTotalRolls.getTotalPowerAndRolls(
              Map.of(
                  mock(Unit.class),
                  TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(1).build(),
                  mock(Unit.class),
                  TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(1).build()),
              gameData),
          is(TotalPowerAndTotalRolls.builder().totalPower(5).totalRolls(2).build()));
    }

    @Test
    void rollIsMultipliedWithPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(3).setAttackRolls(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setAttack(2).setAttackRolls(2);

      assertThat(
          TotalPowerAndTotalRolls.getTotalPowerAndRolls(
              Map.of(
                  unit,
                  TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(2).build(),
                  unit2,
                  TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build()),
              gameData),
          is(TotalPowerAndTotalRolls.builder().totalPower(10).totalRolls(4).build()));
    }

    @Test
    @DisplayName("If the power is more than the dice sides, then dice sides will be used")
    void individualPowerIsLimitedToDiceSides() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(8).setAttackRolls(2);

      assertThat(
          TotalPowerAndTotalRolls.getTotalPowerAndRolls(
              Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(8).totalRolls(2).build()),
              gameData),
          is(TotalPowerAndTotalRolls.builder().totalPower(12).totalRolls(2).build()));
    }

    @ParameterizedTest
    @MethodSource("bestRollSimulated")
    void lhtrIsSimulatedWithALittleExtraPower(
        final int power,
        final int rolls,
        final int diceSides,
        final int expectedPower,
        final int expectedRolls) {
      final GameData gameData =
          givenGameData().withDiceSides(diceSides).withLhtrHeavyBombers(true).build();

      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(power).setAttackRolls(rolls);

      assertThat(
          TotalPowerAndTotalRolls.getTotalPowerAndRolls(
              Map.of(
                  unit,
                  TotalPowerAndTotalRolls.builder().totalPower(power).totalRolls(rolls).build()),
              gameData),
          is(
              TotalPowerAndTotalRolls.builder()
                  .totalPower(expectedPower)
                  .totalRolls(expectedRolls)
                  .build()));
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
        final int power,
        final int rolls,
        final int diceSides,
        final int expectedPower,
        final int expectedRolls) {
      final GameData gameData = givenGameData().withDiceSides(diceSides).build();

      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(power).setAttackRolls(rolls).setChooseBestRoll(true);

      assertThat(
          TotalPowerAndTotalRolls.getTotalPowerAndRolls(
              Map.of(
                  unit,
                  TotalPowerAndTotalRolls.builder().totalPower(power).totalRolls(rolls).build()),
              gameData),
          is(
              TotalPowerAndTotalRolls.builder()
                  .totalPower(expectedPower)
                  .totalRolls(expectedRolls)
                  .build()));
    }
  }

  @Nested
  class GetTotalAaAttacks {

    @Test
    void emptyPowerMapIsZeroAttacks() {
      assertThat(
          TotalPowerAndTotalRolls.getTotalAaAttacks(Map.of(), List.of(mock(Unit.class))), is(0));
    }

    @Test
    void emptyUnitCollectionIsZeroAttacks() {
      assertThat(
          TotalPowerAndTotalRolls.getTotalAaAttacks(
              Map.of(
                  mock(Unit.class),
                  TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()),
              List.of()),
          is(0));
    }

    @Test
    void powerMapWithNoPowerOrRollsIsZeroAttacks() {
      assertThat(
          TotalPowerAndTotalRolls.getTotalAaAttacks(
              Map.of(
                  mock(Unit.class),
                  TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(10).build(),
                  mock(Unit.class),
                  TotalPowerAndTotalRolls.builder().totalPower(10).totalRolls(0).build()),
              List.of(mock(Unit.class))),
          is(0));
    }

    @Test
    void unitWithInfiniteRollsMeansAttacksEqualToTarget() {
      final GameData gameData = givenGameData().build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      assertThat(
          TotalPowerAndTotalRolls.getTotalAaAttacks(
              Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(-1).build()),
              List.of(mock(Unit.class), mock(Unit.class), mock(Unit.class))),
          is(3));
    }

    @Test
    void multipleUnitsWithInfiniteRollsMeansAttacksEqualToTarget() {
      final GameData gameData = givenGameData().build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      assertThat(
          "Infinite unit means all enemies are rolled for but no overstacking",
          TotalPowerAndTotalRolls.getTotalAaAttacks(
              Map.of(
                  unit,
                  TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(-1).build(),
                  unit2,
                  TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(-1).build()),
              List.of(mock(Unit.class), mock(Unit.class), mock(Unit.class))),
          is(3));
    }

    @Test
    void infiniteUnitAndNonInfiniteUnitMeansAttacksEqualsToTarget() {
      final GameData gameData = givenGameData().build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(10);
      assertThat(
          "Infinite unit means all enemies are rolled for",
          TotalPowerAndTotalRolls.getTotalAaAttacks(
              Map.of(
                  unit,
                  TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(-1).build(),
                  unit2,
                  TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(10).build()),
              List.of(mock(Unit.class), mock(Unit.class), mock(Unit.class))),
          is(3));
    }

    @Test
    void rollsOfNonInfiniteUnitEqualsAttack() {
      final GameData gameData = givenGameData().build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);
      assertThat(
          "Unit only has one roll",
          TotalPowerAndTotalRolls.getTotalAaAttacks(
              Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build()),
              List.of(mock(Unit.class), mock(Unit.class), mock(Unit.class))),
          is(1));
    }

    @Test
    void rollsOfNonInfiniteUnitGreaterThanTargetCountMeansAttackEqualsTarget() {
      final GameData gameData = givenGameData().build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(2);
      assertThat(
          "There is only 3 units and no overstack so only allow 3",
          TotalPowerAndTotalRolls.getTotalAaAttacks(
              Map.of(
                  unit,
                  TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(2).build(),
                  unit2,
                  TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(2).build()),
              List.of(mock(Unit.class), mock(Unit.class), mock(Unit.class))),
          is(3));
    }

    @Test
    void overstackUnitCanCauseAttackToGoOverTargetCount() {
      final GameData gameData = givenGameData().build();
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

      assertThat(
          "Infinite gives total attacks equal to number of units (3)"
              + " and the overstacked unit adds 2 more",
          TotalPowerAndTotalRolls.getTotalAaAttacks(
              Map.of(
                  overstackUnit,
                  TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(2).build(),
                  unit,
                  TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(3).build(),
                  unit2,
                  TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(-1).build()),
              List.of(mock(Unit.class), mock(Unit.class), mock(Unit.class))),
          is(5));
    }
  }
}
