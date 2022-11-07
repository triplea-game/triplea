package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.dice.calculator.RolledDice;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AaPowerStrengthAndRollsTest {

  @Mock GamePlayer owner;

  private Unit givenUnit(final String name, final GameData gameData) {
    return givenUnit(givenUnitType(name, gameData));
  }

  private Unit givenUnit(final UnitType unitType) {
    return unitType.createTemp(1, owner).get(0);
  }

  private UnitType givenUnitType(final String name, final GameData gameData) {
    final UnitType unitType = new UnitType(name + "Type", gameData);
    final UnitAttachment unitAttachment =
        new UnitAttachment(name + "Attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    return unitType;
  }

  private AaPowerStrengthAndRolls givenAaPowerStrengthAndRolls(
      final List<Unit> units, final int numValidTargets) {
    return AaPowerStrengthAndRolls.build(
        units,
        numValidTargets,
        AaOffenseCombatValue.builder()
            .rollSupportFromFriends(AvailableSupports.EMPTY_RESULT)
            .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
            .strengthSupportFromFriends(AvailableSupports.EMPTY_RESULT)
            .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
            .build());
  }

  @Nested
  class GetRolls {

    @Test
    void singleAa() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final List<Unit> units = List.of(unit);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 2);

      assertThat("Unit has 1 roll and there are is least 1 target", result.getRolls(unit), is(1));
    }

    @Test
    void singleAaWithMoreRollsThanTargets() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(3);
      final List<Unit> units = List.of(unit);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 2);

      assertThat("Unit has 3 rolls but only 2 targets", result.getRolls(unit), is(2));
    }

    @Test
    void twoAa() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);

      final List<Unit> units = List.of(unit1, unit2);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat("Unit1 has 1 roll and there is 4 targets", result.getRolls(unit1), is(1));
      assertThat("Unit2 has 1 roll and there is 4 targets", result.getRolls(unit2), is(1));
    }

    @Test
    void twoAaWithDifferentPowerAndMoreRollsThanTargets() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2);

      final List<Unit> units = List.of(unit1, unit2);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 3);

      assertThat(
          "Unit2 has higher strength so can use both its rolls", result.getRolls(unit2), is(2));
      assertThat("Unit1 can only use one of its rolls", result.getRolls(unit1), is(1));
    }

    @Test
    void threeAaWithDifferentPowerAndMoreRollsThanTargets() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(1).setMaxAaAttacks(2);

      final List<Unit> units = List.of(unit1, unit2, unit3);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 3);

      assertThat(
          "Unit2 has higher strength so can use both its rolls", result.getRolls(unit2), is(2));
      assertThat("Unit1 can only use one of its rolls", result.getRolls(unit1), is(1));
      assertThat("Unit3 can not use any of its rolls", result.getRolls(unit3), is(0));
    }

    @Test
    void oneAaWithInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat("Infinite unit can roll for all targets", result.getRolls(unit), is(4));
    }

    @Test
    void twoAaWithInfiniteWithDifferentPower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit1, unit2);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat(
          "Unit2 is stronger so it rolls for all the targets", result.getRolls(unit2), is(4));
      assertThat("Unit1 is weaker so it doesn't roll at all", result.getRolls(unit1), is(0));
    }

    @Test
    void twoAaWithInfiniteWithDifferentDice() {
      final GameData gameData = givenGameData().build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1
          .getUnitAttachment()
          .setOffensiveAttackAa(2)
          .setMaxAaAttacks(-1)
          .setOffensiveAttackAaMaxDieSides(4);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2
          .getUnitAttachment()
          .setOffensiveAttackAa(3)
          .setMaxAaAttacks(-1)
          .setOffensiveAttackAaMaxDieSides(8);
      final List<Unit> units = List.of(unit1, unit2);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat(
          "2 of 4 is better than 3 of 8 so unit1 rolls for all targets",
          result.getRolls(unit1),
          is(4));
      assertThat("Unit2 is weaker so it doesn't roll", result.getRolls(unit2), is(0));
    }

    @Test
    void twoAaWithOneRollAndInfiniteSamePower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit1, unit2);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat(
          "Unit1 is equal power to the infinite unit2 so it doesn't roll",
          result.getRolls(unit1),
          is(0));
      assertThat("Unit2 is infinite so it rolls for all targets", result.getRolls(unit2), is(4));
    }

    @Test
    void twoAaWithOneRollAndInfiniteWhereInfiniteIsHigher() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit1, unit2);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat(
          "Unit1 is weaker than the infinite unit2 so it doesn't roll",
          result.getRolls(unit1),
          is(0));
      assertThat("Unit2 is infinite so it rolls for all targets", result.getRolls(unit2), is(4));
    }

    @Test
    void twoAaWithOneRollAndInfiniteWhereInfiniteIsLower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final List<Unit> units = List.of(unit1, unit2);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat(
          "Unit1 is stronger than the infinite unit2 so it rolls once",
          result.getRolls(unit1),
          is(1));
      assertThat(
          "Unit2 is infinite so it rolls for the other targets", result.getRolls(unit2), is(3));
    }

    @Test
    void oneAaWithOverStackAndMoreRollsThanTargets() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(5).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat(
          "Overstack always uses all of its rolls even if there are not enough targets",
          result.getRolls(unit),
          is(5));
    }

    @Test
    void oneAaWithOverstackAndInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit = givenUnit("test", gameData);
      unit.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 2);

      assertThat(
          "Overstack makes no sense on an infinite unit so it only rolls for all the targets",
          result.getRolls(unit),
          is(2));
    }

    @Test
    void oneOverstackAndOneInfinite() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit1, unit2);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat(
          "Unit1 is infinite so it rolls for all the targets", result.getRolls(unit1), is(4));
      assertThat("Unit2 is overstack so it just uses all its rolls", result.getRolls(unit2), is(2));
    }

    @Test
    void oneOverstackAndOneNormal() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit1, unit2);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat("Unit1 has two rolls and there are 4 targets", result.getRolls(unit1), is(2));
      assertThat("Unit2 is overstack so it just uses all its rolls", result.getRolls(unit2), is(2));
    }

    @Test
    void oneOverstackOneInfiniteAndOneNormalSamePower() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit1, unit2, unit3);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat(
          "Unit1 is infinite and equal strength as the non-infinite so it rolls for all targets",
          result.getRolls(unit1),
          is(4));
      assertThat(
          "Unit2 is normal but since it has equal strength to infinite, it doesn't roll",
          result.getRolls(unit2),
          is(0));
      assertThat("Unit3 is overstack so it just uses all its rolls", result.getRolls(unit3), is(2));
    }

    @Test
    void oneOverstackOneInfiniteAndOneNormalDifferentPowersWhereNormalIsBest() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit1, unit2, unit3);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat(
          "Unit1 is infinite and weaker than the non infinite, so it only rolls for 2 targets",
          result.getRolls(unit1),
          is(2));
      assertThat(
          "Unit2 is normal and is stronger than the infinite so it uses both its rolls",
          result.getRolls(unit2),
          is(2));
      assertThat("Unit3 is overstack so it just uses all its rolls", result.getRolls(unit3), is(2));
    }

    @Test
    void oneOverstackOneInfiniteAndOneNormalDifferentPowersWhereNormalIsWorst() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit1, unit2, unit3);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);

      assertThat(
          "Unit1 is infinite and stronger than the non infinite, so it rolls for all targets",
          result.getRolls(unit1),
          is(4));
      assertThat(
          "Unit2 is normal and is weaker than the infinite so it doesn't roll",
          result.getRolls(unit2),
          is(0));
      assertThat("Unit3 is overstack so it just uses all its rolls", result.getRolls(unit3), is(2));
    }
  }

  @Nested
  class GetDiceHits {
    @Test
    void oneOverstackOneInfiniteAndOneNormalDifferentPowersWhereNormalIsBest() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setOffensiveAttackAa(2).setMaxAaAttacks(-1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setOffensiveAttackAa(3).setMaxAaAttacks(2);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setOffensiveAttackAa(4).setMaxAaAttacks(2).setMayOverStackAa(true);
      final List<Unit> units = List.of(unit1, unit2, unit3);

      final AaPowerStrengthAndRolls result = givenAaPowerStrengthAndRolls(units, 4);
      final int[] dice = {0, 5, 1, 4, 2, 4};
      final List<Die> diceHits = RolledDice.getDiceHits(dice, result.getActiveUnits());

      assertThat(
          "Normal unit rolls twice but only hits on the 0, not the 5. Infinite unit rolls "
              + "twice (because only 2 more targets) but only hits on the 1, not the 4. Overstack "
              + "unit rolls twice but only hits on the 2, not the 4.",
          diceHits,
          is(
              List.of(
                  new Die(0, 3, Die.DieType.HIT),
                  new Die(5, 3, Die.DieType.MISS),
                  new Die(1, 2, Die.DieType.HIT),
                  new Die(4, 2, Die.DieType.MISS),
                  new Die(2, 4, Die.DieType.HIT),
                  new Die(4, 4, Die.DieType.MISS))));
    }
  }
}
