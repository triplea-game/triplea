package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.RULES_ATTACHMENT_NAME;
import static games.strategy.triplea.Constants.TERRITORYEFFECT_ATTACHMENT_NAME;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.attachments.TerritoryEffectAttachment.COMBAT_DEFENSE_EFFECT;
import static games.strategy.triplea.attachments.TerritoryEffectAttachment.COMBAT_OFFENSE_EFFECT;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.GameDataTestUtil;
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
    void attackUnitWithNoSupport() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);

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
          is(TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build()));
    }

    @Test
    void defenseUnitWithNoSupport() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttackAa(1).setMaxAaAttacks(1);

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build()));
    }

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
    void attackUnitWithOneStrengthSupportFromFriendly() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAstrength")
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(1).build()));
    }

    @Test
    void attackUnitWithOneRollSupportFromFriendly() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAroll")
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(2).build()));
    }

    @Test
    void attackUnitWithOneStrengthSupportFromEnemy() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAstrength")
              .setBonus(-1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(enemySupport)
                  .build());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));
    }

    @Test
    void attackUnitWithOneRollSupportFromEnemy() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAroll")
              .setBonus(-1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(enemySupport)
                  .build());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));
    }

    @Test
    void attackUnitWithOneSupportForBothRollAndStrength() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAstrength:AAroll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build()));
    }

    @Test
    void twoAttackUnitsWithOnlyOneSupportAvailable() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);
      final Unit nonSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAstrength:AAroll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit, nonSupportedUnit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          "The non supported unit should not get any bonus",
          result,
          is(
              Map.of(
                  unit, TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  nonSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build())));
    }

    @Test
    void threeAttackUnitsWithOneSupportAvailableThatAffectsTwo() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);
      final Unit otherSupportedUnit = givenUnit(unitType);
      otherSupportedUnit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);
      final Unit nonSupportedUnit = givenUnit(unitType);
      nonSupportedUnit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAstrength:AAroll")
              .setNumber(2)
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit, otherSupportedUnit, nonSupportedUnit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          "The non supported unit should not get any bonus",
          result,
          is(
              Map.of(
                  unit, TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  otherSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  nonSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build())));
    }

    @Test
    void attackUnitsWithMultipleSupportUnits() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType);
      unit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);
      final Unit otherSupportedUnit = givenUnit(unitType);
      otherSupportedUnit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);
      final Unit nonSupportedUnit = givenUnit(unitType);
      nonSupportedUnit.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAstrength:AAroll")
              .setNumber(2)
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));
      final Unit supportUnit2 = givenUnit("support2", gameData);
      final UnitSupportAttachment unitSupportAttachment2 =
          givenUnitSupportAttachment(gameData, supportUnit2.getType(), "test2", "AAstrength:AAroll")
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
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit, otherSupportedUnit, nonSupportedUnit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          "First should have both support, second should have one support, "
              + "last should have no support",
          result,
          is(
              Map.of(
                  unit, TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(3).build(),
                  otherSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  nonSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build())));
    }

    @Test
    void maxPowerIsDiceSidesAfterAllSupports() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAstrength")
              .setBonus(4)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .build());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(6).totalRolls(1).build()));
    }

    @Test
    void minPowerIsZeroAfterAllSupports() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAstrength")
              .setBonus(-8)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(enemySupport)
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
          result,
          is(
              Map.of(
                  strongUnit,
                  TotalPowerAndTotalRolls.builder().totalPower(5).totalRolls(2).build(),
                  lessWeakUnit,
                  TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(1).build(),
                  weakUnit,
                  TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(1).build())));
    }

    @Test
    void infiniteRollIgnoresSupport() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(-1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "AAstrength:AAroll")
              .setBonus(2)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Unit enemyUnit = givenUnit("support", gameData);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, enemyUnit.getType(), "test2", "AAstrength:AAroll")
              .setBonus(-1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemyUnit), List.of(enemyUnitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(enemySupport)
                  .build());

      assertThat(
          "Infinite rolls isn't affected by the support",
          result,
          is(Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(5).totalRolls(-1).build())));
    }

    @Test
    void minRollsIsZero() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(1);

      final Unit enemyUnit = givenUnit("support", gameData);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, enemyUnit.getType(), "test", "AAstrength:AAroll")
              .setBonus(-2)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemyUnit), List.of(enemyUnitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getAaUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              AaOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(enemySupport)
                  .build());

      assertThat(
          "The support should take rolls to -1 but the min is 0",
          result,
          is(Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build())));
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class GetUnitPowerAndRollsForNormalBattles {

    @Test
    void attackUnitWithNoSupport() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryIsLand(true)
                  .territoryEffects(List.of())
                  .build(),
              Map.of(),
              Map.of());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build()));
    }

    @Test
    void defenseUnitWithNoSupport() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build(),
              Map.of(),
              Map.of());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build()));
    }

    @Test
    void attackMarineWithNoSupport() throws MutableProperty.InvalidValueException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1).setIsMarine(1);
      unit.getPropertyOrThrow(Unit.UNLOADED_AMPHIBIOUS).setValue(true);

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
          "Offense adds marine bonus for amphibious assaults",
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(1).build()));
    }

    // defenders should actually never have UNLOADED_AMPHIBIOUS set to true but this tests
    // that even if it happens, the marine bonus is not added
    @Test
    void defenseMarineWithNoSupport() throws MutableProperty.InvalidValueException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1).setIsMarine(1);
      unit.getPropertyOrThrow(Unit.UNLOADED_AMPHIBIOUS).setValue(true);

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build(),
              Map.of(),
              Map.of());

      assertThat(
          "Defense doesn't use marine bonus",
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build()));
    }

    @Test
    void attackBombardmentWithNoSupport() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1).setBombard(3).setIsSea(true);

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
          "Offense uses bombardment when it is a sea unit in a land battle",
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(1).build()));
    }

    @Test
    void defenseBombardmentWithNoSupport() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1).setBombard(3).setIsSea(true);

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build(),
              Map.of(),
              Map.of());

      assertThat(
          "Defense doesn't use the bombardment value",
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build()));
    }

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
    void attackUnitWithOneStrengthSupportFromFriendly() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
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
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(1).build()));

      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportPowerMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
      assertThat(unitSupportRollsMap, is(Map.of()));
    }

    @Test
    void defenseUnitWithOneStrengthSupportFromFriendly() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(1).build()));

      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportPowerMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
      assertThat(unitSupportRollsMap, is(Map.of()));
    }

    @Test
    void attackUnitWithOneRollSupportFromFriendly() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "roll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
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
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(2).build()));

      assertThat(unitSupportPowerMap, is(Map.of()));
      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportRollsMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
    }

    @Test
    void defenseUnitWithOneRollSupportFromFriendly() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "roll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(2).build()));

      assertThat(unitSupportPowerMap, is(Map.of()));
      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportRollsMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
    }

    @Test
    void attackUnitWithOneStrengthSupportFromEnemy() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit enemyUnit = givenUnit("support", gameData);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, enemyUnit.getType(), "test", "strength")
              .setBonus(-1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemyUnit), List.of(enemyUnitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(enemySupport)
                  .territoryEffects(List.of())
                  .territoryIsLand(true)
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));

      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportPowerMap,
          is(Map.of(enemyUnit, new IntegerMap<>(Map.of(unit, -1)))));
      assertThat(unitSupportRollsMap, is(Map.of()));
    }

    @Test
    void defenseUnitWithOneStrengthSupportFromEnemy() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit enemyUnit = givenUnit("support", gameData);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, enemyUnit.getType(), "test", "strength")
              .setBonus(-1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemyUnit), List.of(enemyUnitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(enemySupport)
                  .territoryEffects(List.of())
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));

      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportPowerMap,
          is(Map.of(enemyUnit, new IntegerMap<>(Map.of(unit, -1)))));
      assertThat(unitSupportRollsMap, is(Map.of()));
    }

    @Test
    void attackUnitWithOneRollSupportFromEnemy() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit enemyUnit = givenUnit("support", gameData);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, enemyUnit.getType(), "test", "roll")
              .setBonus(-1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemyUnit), List.of(enemyUnitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(enemySupport)
                  .territoryEffects(List.of())
                  .territoryIsLand(true)
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));

      assertThat(unitSupportPowerMap, is(Map.of()));
      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportRollsMap,
          is(Map.of(enemyUnit, new IntegerMap<>(Map.of(unit, -1)))));
    }

    @Test
    void defenseUnitWithOneRollSupportFromEnemy() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit enemyUnit = givenUnit("support", gameData);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, enemyUnit.getType(), "test", "roll")
              .setBonus(-1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemyUnit), List.of(enemyUnitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(enemySupport)
                  .territoryEffects(List.of())
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));

      assertThat(unitSupportPowerMap, is(Map.of()));
      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportRollsMap,
          is(Map.of(enemyUnit, new IntegerMap<>(Map.of(unit, -1)))));
    }

    @Test
    void attackUnitWithOneSupportForBothRollAndStrength() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength:roll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
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
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build()));

      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportPowerMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportRollsMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
    }

    @Test
    void defenseUnitWithOneSupportForBothRollAndStrength() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength:roll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build()));

      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportPowerMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportRollsMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
    }

    @Test
    void twoAttackUnitsWithOnlyOneSupportAvailable() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);
      final Unit nonSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength:roll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit, nonSupportedUnit),
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
          "The non supported unit should not get any bonus",
          result,
          is(
              Map.of(
                  unit, TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  nonSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build())));

      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportPowerMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportRollsMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
    }

    @Test
    void twoDefenseUnitsWithOnlyOneSupportAvailable() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);
      final Unit nonSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength:roll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit, nonSupportedUnit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          "The non supported unit should not get any bonus",
          result,
          is(
              Map.of(
                  unit, TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  nonSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build())));

      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportPowerMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
      assertThat(
          "The support unit gave 1 bonus to the unit",
          unitSupportRollsMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
    }

    @Test
    void threeAttackUnitsWithOneSupportAvailableThatAffectsTwo() throws GameParseException {
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
              .setBonus(1)
              .setNumber(2)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

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
          "The non supported unit should not get any bonus",
          result,
          is(
              Map.of(
                  unit, TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  otherSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  nonSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build())));

      assertThat(
          "The support unit gave 1 bonus to both units",
          unitSupportPowerMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1, otherSupportedUnit, 1)))));
      assertThat(
          "The support unit gave 1 bonus to both units",
          unitSupportRollsMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1, otherSupportedUnit, 1)))));
    }

    @Test
    void threeDefenseUnitsWithOneSupportThatAffectsTwo() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);
      final Unit otherSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);
      final Unit nonSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength:roll")
              .setNumber(2)
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit, otherSupportedUnit, nonSupportedUnit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          "The non supported unit should not get any bonus",
          result,
          is(
              Map.of(
                  unit, TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  otherSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  nonSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build())));

      assertThat(
          "The support unit gave 1 bonus to both units",
          unitSupportPowerMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1, otherSupportedUnit, 1)))));
      assertThat(
          "The support unit gave 1 bonus to both units",
          unitSupportRollsMap,
          is(Map.of(supportUnit, new IntegerMap<>(Map.of(unit, 1, otherSupportedUnit, 1)))));
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
          "First should have both support, second should have one support, "
              + "last should have no support",
          result,
          is(
              Map.of(
                  unit, TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(3).build(),
                  otherSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  nonSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build())));

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

    @Test
    void defenseUnitsWithMultipleSupportUnits() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);
      final Unit otherSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);
      final Unit nonSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);

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
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          "First should have both support, second should have one support, "
              + "last should have no support",
          result,
          is(
              Map.of(
                  unit, TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(3).build(),
                  otherSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  nonSupportedUnit,
                      TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build())));

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

    @Test
    void offenseUnitsWithSupportThatHasTwoAttachmentsAndBonusCountOf1() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);
      final Unit otherSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength:roll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));
      final Unit supportUnit2 = givenUnit("support2", gameData);
      final UnitSupportAttachment unitSupportAttachment2 =
          givenUnitSupportAttachment(gameData, supportUnit2.getType(), "test2", "strength:roll")
              .setBonusType(unitSupportAttachment.getBonusType())
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
              List.of(unit, otherSupportedUnit),
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
          "No stack on the same unit so both units get the bonus",
          result,
          is(
              Map.of(
                  unit,
                  TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build(),
                  otherSupportedUnit,
                  TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(2).build())));

      assertThat(
          "No stack on the same unit so both units get the bonus",
          unitSupportPowerMap,
          is(
              Map.of(
                  supportUnit,
                  new IntegerMap<>(Map.of(unit, 1)),
                  supportUnit2,
                  new IntegerMap<>(Map.of(otherSupportedUnit, 1)))));
      assertThat(
          "No stack on the same unit so both units get the bonus",
          unitSupportRollsMap,
          is(
              Map.of(
                  supportUnit,
                  new IntegerMap<>(Map.of(unit, 1)),
                  supportUnit2,
                  new IntegerMap<>(Map.of(otherSupportedUnit, 1)))));
    }

    @Test
    void offenseUnitsWithSupportThatHasTwoAttachmentsAndBonusCountOf2() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final UnitType unitType = givenUnitType("test", gameData);
      final Unit unit = givenUnit(unitType);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);
      final Unit otherSupportedUnit = givenUnit(unitType);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength:roll")
              .setBonus(1)
              .setBonusType(new UnitSupportAttachment.BonusType("same", 2))
              .setUnitType(Set.of(unit.getType()));
      final Unit supportUnit2 = givenUnit("support2", gameData);
      final UnitSupportAttachment unitSupportAttachment2 =
          givenUnitSupportAttachment(gameData, supportUnit2.getType(), "test2", "strength:roll")
              .setBonusType(unitSupportAttachment.getBonusType())
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
              List.of(unit, otherSupportedUnit),
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
          "The first unit gets all the bonus because bonus count is 2",
          result,
          is(
              Map.of(
                  unit,
                  TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(3).build(),
                  otherSupportedUnit,
                  TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(1).build())));

      assertThat(
          "The first unit gets all the bonus because bonus count is 2",
          unitSupportPowerMap,
          is(
              Map.of(
                  supportUnit,
                  new IntegerMap<>(Map.of(unit, 1)),
                  supportUnit2,
                  new IntegerMap<>(Map.of(unit, 1)))));
      assertThat(
          "The first unit gets all the bonus because bonus count is 2",
          unitSupportRollsMap,
          is(
              Map.of(
                  supportUnit,
                  new IntegerMap<>(Map.of(unit, 1)),
                  supportUnit2,
                  new IntegerMap<>(Map.of(unit, 1)))));
    }

    @Test
    void attackShouldNotHaveFirstTurnLimiting() {
      final GameData gameData = givenGameData().withDiceSides(6).build();

      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(3).setAttackRolls(3);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

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
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          "First turn limiting should not happen",
          result,
          is(Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(3).build())));

      // since the code is never called, adding mocks to return the correct values will throw errors
      // unless lenient() is added.  So just check that the call is never made.
      verify(
              gameData,
              never()
                  .description(
                      "Game sequence should only be called for first turn limiting check "
                          + "and attackers don't get limited"))
          .getSequence();

      assertThat(unitSupportPowerMap, is(Map.of()));
      assertThat(unitSupportRollsMap, is(Map.of()));
    }

    @Test
    void defenseWithFirstTurnLimited() {
      final GamePlayer attacker = mock(GamePlayer.class);
      final RulesAttachment rulesAttachment = mock(RulesAttachment.class);
      when(rulesAttachment.getDominatingFirstRoundAttack()).thenReturn(true);
      when(attacker.getAttachment(RULES_ATTACHMENT_NAME)).thenReturn(rulesAttachment);

      final GameData gameData = givenGameData().withDiceSides(6).withRound(1, attacker).build();

      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(3).setDefenseRolls(3);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of())
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          "First turn limiting should reduce power to 1",
          result,
          is(Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(1).totalRolls(3).build())));

      assertThat(unitSupportPowerMap, is(Map.of()));
      assertThat(unitSupportRollsMap, is(Map.of()));
    }

    @Test
    void defenseWithFirstTurnLimitedWithSupport()
        throws MutableProperty.InvalidValueException, GameParseException {
      final GamePlayer attacker = mock(GamePlayer.class);
      final RulesAttachment rulesAttachment = mock(RulesAttachment.class);
      when(rulesAttachment.getDominatingFirstRoundAttack()).thenReturn(true);
      when(attacker.getAttachment(RULES_ATTACHMENT_NAME)).thenReturn(rulesAttachment);

      final GameData gameData = givenGameData().withDiceSides(6).withRound(1, attacker).build();

      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(3).setDefenseRolls(3);

      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength:roll")
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final Unit enemyUnit = givenUnit("support2", gameData);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, enemyUnit.getType(), "test2", "strength:roll")
              // use a bonus of 1 for the enemy support to ensure that the value doesn't go to 0 and
              // the
              // support can be detected
              .setBonus(1)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemyUnit), List.of(enemyUnitSupportAttachment), false, true));

      final TerritoryEffect territoryEffect = new TerritoryEffect("test", gameData);
      final TerritoryEffectAttachment territoryEffectAttachment =
          new TerritoryEffectAttachment("test", territoryEffect, gameData);
      territoryEffect.addAttachment(TERRITORYEFFECT_ATTACHMENT_NAME, territoryEffectAttachment);
      territoryEffectAttachment
          .getPropertyOrThrow(COMBAT_DEFENSE_EFFECT)
          .setValue(new IntegerMap<>(Map.of(unit.getType(), 1)));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(enemySupport)
                  .territoryEffects(List.of(territoryEffect))
                  .build(),
              unitSupportPowerMap,
              unitSupportRollsMap);

      assertThat(
          "First turn limiting should reduce power to 1 and enemy support should increase it to 2"
              + " and territory effect should increase it to 3",
          result,
          is(Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(3).totalRolls(5).build())));

      assertThat(
          "Enemy support unit should be the only one giving strength support",
          unitSupportPowerMap,
          is(Map.of(enemyUnit, new IntegerMap<>(Map.of(unit, 1)))));
      assertThat(
          "Both support units give support for roll",
          unitSupportRollsMap,
          is(
              Map.of(
                  enemyUnit, new IntegerMap<>(Map.of(unit, 1)),
                  supportUnit, new IntegerMap<>(Map.of(unit, 1)))));
    }

    @Test
    void attackUnitWithTerritoryEffects() throws MutableProperty.InvalidValueException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(1).setAttackRolls(1);

      final TerritoryEffect territoryEffect = new TerritoryEffect("test", gameData);
      final TerritoryEffectAttachment territoryEffectAttachment =
          new TerritoryEffectAttachment("test", territoryEffect, gameData);
      territoryEffect.addAttachment(TERRITORYEFFECT_ATTACHMENT_NAME, territoryEffectAttachment);
      territoryEffectAttachment
          .getPropertyOrThrow(COMBAT_OFFENSE_EFFECT)
          .setValue(new IntegerMap<>(Map.of(unit.getType(), 1)));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of(territoryEffect))
                  .territoryIsLand(true)
                  .build(),
              Map.of(),
              Map.of());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(1).build()));
    }

    @Test
    void defenseUnitWithTerritoryEffects() throws MutableProperty.InvalidValueException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setDefense(1).setDefenseRolls(1);

      final TerritoryEffect territoryEffect = new TerritoryEffect("test", gameData);
      final TerritoryEffectAttachment territoryEffectAttachment =
          new TerritoryEffectAttachment("test", territoryEffect, gameData);
      territoryEffect.addAttachment(TERRITORYEFFECT_ATTACHMENT_NAME, territoryEffectAttachment);
      territoryEffectAttachment
          .getPropertyOrThrow(COMBAT_DEFENSE_EFFECT)
          .setValue(new IntegerMap<>(Map.of(unit.getType(), 1)));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainDefenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of(territoryEffect))
                  .build(),
              Map.of(),
              Map.of());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(2).totalRolls(1).build()));
    }

    @Test
    void maxPowerIsDiceSidesAfterAllSupports()
        throws MutableProperty.InvalidValueException, GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(3).setAttackRolls(1);

      final Unit supportUnit = givenUnit("support", gameData);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, supportUnit.getType(), "test", "strength")
              .setBonus(3)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(unitSupportAttachment), false, true));

      final TerritoryEffect territoryEffect = new TerritoryEffect("test", gameData);
      final TerritoryEffectAttachment territoryEffectAttachment =
          new TerritoryEffectAttachment("test", territoryEffect, gameData);
      territoryEffect.addAttachment(TERRITORYEFFECT_ATTACHMENT_NAME, territoryEffectAttachment);
      territoryEffectAttachment
          .getPropertyOrThrow(COMBAT_OFFENSE_EFFECT)
          .setValue(new IntegerMap<>(Map.of(unit.getType(), 3)));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(friendlySupport)
                  .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
                  .territoryEffects(List.of(territoryEffect))
                  .territoryIsLand(true)
                  .build(),
              new HashMap<>(),
              new HashMap<>());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(6).totalRolls(1).build()));
    }

    @Test
    void minPowerIsZeroAfterAllSupports()
        throws MutableProperty.InvalidValueException, GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(3).setAttackRolls(1);

      final Unit enemyUnit = givenUnit("support", gameData);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, enemyUnit.getType(), "test", "strength")
              .setBonus(-3)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemyUnit), List.of(enemyUnitSupportAttachment), false, true));

      final TerritoryEffect territoryEffect = new TerritoryEffect("test", gameData);
      final TerritoryEffectAttachment territoryEffectAttachment =
          new TerritoryEffectAttachment("test", territoryEffect, gameData);
      territoryEffect.addAttachment(TERRITORYEFFECT_ATTACHMENT_NAME, territoryEffectAttachment);
      territoryEffectAttachment
          .getPropertyOrThrow(COMBAT_OFFENSE_EFFECT)
          .setValue(new IntegerMap<>(Map.of(unit.getType(), -3)));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(enemySupport)
                  .territoryEffects(List.of(territoryEffect))
                  .territoryIsLand(true)
                  .build(),
              new HashMap<>(),
              new HashMap<>());

      assertThat(
          result.get(unit),
          is(TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build()));
    }

    @Test
    void minRollsIsZero() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setAttack(4).setAttackRolls(1);

      final Unit enemyUnit = givenUnit("support", gameData);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, enemyUnit.getType(), "test", "strength:roll")
              .setBonus(-2)
              .setUnitType(Set.of(unit.getType()));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemyUnit), List.of(enemyUnitSupportAttachment), false, true));

      final Map<Unit, TotalPowerAndTotalRolls> result =
          TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
              List.of(unit),
              MainOffenseCombatValue.builder()
                  .gameData(gameData)
                  .supportFromFriends(AvailableSupports.EMPTY_RESULT)
                  .supportFromEnemies(enemySupport)
                  .territoryEffects(List.of())
                  .territoryIsLand(true)
                  .build(),
              new HashMap<>(),
              new HashMap<>());

      assertThat(
          "The support should take rolls to -1 but the minimum is 0",
          result,
          is(Map.of(unit, TotalPowerAndTotalRolls.builder().totalPower(0).totalRolls(0).build())));
    }

    @Test
    void testGetTotalPowerForSupportBonusTypeCount() {
      final GameData twwGameData = TestMapGameData.TWW.getGameData();

      // Move regular units
      final GamePlayer germans = GameDataTestUtil.germany(twwGameData);
      final Territory berlin = territory("Berlin", twwGameData);
      final List<Unit> attackers = new ArrayList<>();

      attackers.addAll(GameDataTestUtil.germanInfantry(twwGameData).create(1, germans));
      attackers.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(1, germans));
      int attackPower =
          TotalPowerAndTotalRolls.getTotalPower(
              TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
                  attackers,
                  CombatValue.buildMain(
                      List.of(), attackers, false, twwGameData, berlin, List.of())),
              twwGameData);
      assertThat("1 artillery should provide +1 support to the infantry", attackPower, is(6));

      attackers.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(1, germans));
      attackPower =
          TotalPowerAndTotalRolls.getTotalPower(
              TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
                  attackers,
                  CombatValue.buildMain(
                      List.of(), attackers, false, twwGameData, berlin, List.of())),
              twwGameData);
      assertThat(
          "2 artillery should provide +2 support to the infantry as stack count is 2",
          attackPower,
          is(10));

      attackers.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(1, germans));
      attackPower =
          TotalPowerAndTotalRolls.getTotalPower(
              TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
                  attackers,
                  CombatValue.buildMain(
                      List.of(), attackers, false, twwGameData, berlin, List.of())),
              twwGameData);
      assertThat(
          "3 artillery should provide +2 support to the infantry as can't provide more than 2",
          attackPower,
          is(13));
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
