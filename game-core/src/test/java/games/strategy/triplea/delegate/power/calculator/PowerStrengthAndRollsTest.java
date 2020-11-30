package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
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
class PowerStrengthAndRollsTest {

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

  private PowerStrengthAndRolls givenPowerStrengthAndRolls(final List<Unit> units) {
    return PowerStrengthAndRolls.build(
        units,
        MainOffenseCombatValue.builder()
            .gameSequence(mock(GameSequence.class))
            .gameDiceSides(6)
            .lhtrHeavyBombers(false)
            .rollSupportFromFriends(AvailableSupports.EMPTY_RESULT)
            .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
            .strengthSupportFromFriends(AvailableSupports.EMPTY_RESULT)
            .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
            .territoryEffects(List.of())
            .friendUnits(units)
            .enemyUnits(List.of())
            .build());
  }

  @Nested
  class GetDiceHits {
    @Test
    void threeUnitsWithDifferentPowers() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setAttack(2).setAttackRolls(1);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setAttack(3).setAttackRolls(1);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setAttack(4).setAttackRolls(1);
      final List<Unit> units = List.of(unit1, unit2, unit3);

      final PowerStrengthAndRolls result = givenPowerStrengthAndRolls(units);
      final int[] dice = {0, 5, 2};
      final List<Die> diceHits = RolledDice.getDiceHits(dice, result.getActiveUnits());

      assertThat(
          "Unit3 rolls first and hits with 0, unit2 rolls second and misses with 5, unit1 rolls"
              + "last and misses with 2",
          diceHits,
          is(
              List.of(
                  new Die(0, 4, Die.DieType.HIT),
                  new Die(5, 3, Die.DieType.MISS),
                  new Die(2, 2, Die.DieType.MISS))));
    }

    @Test
    void unitHasMultipleRolls() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setAttack(2).setAttackRolls(2);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setAttack(3).setAttackRolls(2);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setAttack(4).setAttackRolls(2);
      final List<Unit> units = List.of(unit1, unit2, unit3);

      final PowerStrengthAndRolls result = givenPowerStrengthAndRolls(units);
      final int[] dice = {0, 5, 2, 1, 4, 3};
      final List<Die> diceHits = RolledDice.getDiceHits(dice, result.getActiveUnits());

      assertThat(
          "Unit3 rolls first twice, unit2 rolls second twice, and unit 3 rolls last twice",
          diceHits,
          is(
              List.of(
                  new Die(0, 4, Die.DieType.HIT),
                  new Die(5, 4, Die.DieType.MISS),
                  new Die(2, 3, Die.DieType.HIT),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(4, 2, Die.DieType.MISS),
                  new Die(3, 2, Die.DieType.MISS))));
    }

    @Test
    void unitHasMultipleRollsButCanOnlyChooseBest() {
      final GameData gameData = givenGameData().withDiceSides(6).build();
      final Unit unit1 = givenUnit("test", gameData);
      unit1.getUnitAttachment().setAttack(2).setAttackRolls(2).setChooseBestRoll(true);
      final Unit unit2 = givenUnit("test2", gameData);
      unit2.getUnitAttachment().setAttack(3).setAttackRolls(2).setChooseBestRoll(true);
      final Unit unit3 = givenUnit("test3", gameData);
      unit3.getUnitAttachment().setAttack(4).setAttackRolls(2).setChooseBestRoll(true);
      final List<Unit> units = List.of(unit1, unit2, unit3);

      final PowerStrengthAndRolls result = givenPowerStrengthAndRolls(units);
      final int[] dice = {0, 5, 2, 1, 4, 3};
      final List<Die> diceHits = RolledDice.getDiceHits(dice, result.getActiveUnits());

      assertThat(
          "Unit3 rolls first twice, unit2 rolls second twice, and unit 3 rolls last twice. "
              + "The best of each pair of rolls is used and the other is IGNORED.",
          diceHits,
          is(
              List.of(
                  new Die(0, 4, Die.DieType.HIT),
                  new Die(5, 4, Die.DieType.IGNORED),
                  new Die(2, 3, Die.DieType.IGNORED),
                  new Die(1, 3, Die.DieType.HIT),
                  new Die(4, 2, Die.DieType.IGNORED),
                  new Die(3, 2, Die.DieType.MISS))));
    }
  }
}
