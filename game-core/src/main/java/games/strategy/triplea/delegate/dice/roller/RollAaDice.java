package games.strategy.triplea.delegate.dice.roller;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dice.calculator.LowLuckDice;
import games.strategy.triplea.delegate.dice.calculator.RolledDice;
import games.strategy.triplea.delegate.power.calculator.AaPowerStrengthAndRolls;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.Collection;
import lombok.experimental.UtilityClass;

/** Used to roll AA for battles, SBR, and fly over. */
@UtilityClass
public class RollAaDice {

  /** @param aaUnits Units that are firing. There must be at least one unit. */
  public static DiceRoll rollDice(
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
}
