package games.strategy.triplea.delegate.dice.roller;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dice.calculator.LowLuckDice;
import games.strategy.triplea.delegate.dice.calculator.RolledDice;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.Collection;
import lombok.experimental.UtilityClass;

/** Used to roll dice for attackers and defenders in battles. */
@UtilityClass
public class RollBattleDice {

  public static DiceRoll rollDice(
      final Collection<Unit> units,
      final GamePlayer player,
      final IDelegateBridge bridge,
      final String annotation,
      final CombatValue combatValueCalculator) {

    final DiceRoll diceRoll;
    final PowerStrengthAndRolls unitPowerAndRollsMap =
        PowerStrengthAndRolls.build(units, combatValueCalculator);
    if (Properties.getLowLuck(bridge.getData().getProperties())) {
      diceRoll = LowLuckDice.calculate(unitPowerAndRollsMap, player, bridge::getRandom, annotation);
    } else {
      diceRoll = RolledDice.calculate(unitPowerAndRollsMap, player, bridge::getRandom, annotation);
    }

    bridge
        .getHistoryWriter()
        .addChildToEvent(annotation + " : " + MyFormatter.asDice(diceRoll), diceRoll);
    return diceRoll;
  }
}
