package games.strategy.triplea.delegate.dice;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.dice.calculator.LowLuckDice;
import games.strategy.triplea.delegate.dice.calculator.RolledDice;
import games.strategy.triplea.delegate.power.calculator.AaPowerStrengthAndRolls;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RollDiceFactory {

  /**
   * Used to roll AA for battles, SBR, and fly over.
   *
   * @param aaUnits Units that are firing. There must be at least one unit.
   */
  public static DiceRoll rollAaDice(
      final Collection<Unit> validTargets,
      final Collection<Unit> aaUnits,
      final IDelegateBridge bridge,
      final Territory battleSite,
      final CombatValue combatValueCalculator) {

    final String typeAa = UnitAttachment.get(aaUnits.iterator().next().getType()).getTypeAa();
    final GamePlayer player = aaUnits.iterator().next().getOwner();
    final String annotation =
        player.getName() + " roll " + typeAa + " dice in " + battleSite.getName();

    final AaPowerStrengthAndRolls unitPowerAndRollsMap =
        AaPowerStrengthAndRolls.build(aaUnits, validTargets.size(), combatValueCalculator);

    final DiceRoll diceRoll =
        Properties.getLowLuck(bridge.getData().getProperties())
                || Properties.getLowLuckAaOnly(bridge.getData().getProperties())
            ? LowLuckDice.calculate(unitPowerAndRollsMap, player, bridge::getRandom, annotation)
            : RolledDice.calculate(unitPowerAndRollsMap, player, bridge::getRandom, annotation);

    bridge
        .getHistoryWriter()
        .addChildToEvent(annotation + " : " + MyFormatter.asDice(diceRoll), diceRoll);
    return diceRoll;
  }

  /** Used to roll dice for attackers and defenders in battles. */
  public static DiceRoll rollBattleDice(
      final Collection<Unit> units,
      final GamePlayer player,
      final IDelegateBridge bridge,
      final String annotation,
      final CombatValue combatValueCalculator) {

    final PowerStrengthAndRolls unitPowerAndRollsMap =
        PowerStrengthAndRolls.build(units, combatValueCalculator);

    final DiceRoll diceRoll =
        Properties.getLowLuck(bridge.getData().getProperties())
            ? LowLuckDice.calculate(unitPowerAndRollsMap, player, bridge::getRandom, annotation)
            : RolledDice.calculate(unitPowerAndRollsMap, player, bridge::getRandom, annotation);

    bridge
        .getHistoryWriter()
        .addChildToEvent(annotation + " : " + MyFormatter.asDice(diceRoll), diceRoll);
    return diceRoll;
  }

  /**
   * Roll a specified amount of dice with a specified dice side
   *
   * <p>The rolled value will range from 0 to diceSides - 1
   */
  public static DiceRoll rollNSidedDiceXTimes(
      final IDelegateBridge bridge,
      final int rollCount,
      final int diceSides,
      final GamePlayer playerRolling,
      final IRandomStats.DiceType diceType,
      final String annotation) {
    if (rollCount == 0) {
      return new DiceRoll(new ArrayList<>(), 0, 0, playerRolling.getName());
    }
    final int[] random =
        bridge.getRandom(diceSides, rollCount, playerRolling, diceType, annotation);
    final List<Die> dice = new ArrayList<>();
    for (int i = 0; i < rollCount; i++) {
      dice.add(new Die(random[i], 1, Die.DieType.IGNORED));
    }
    return new DiceRoll(dice, rollCount, rollCount, playerRolling.getName());
  }
}
