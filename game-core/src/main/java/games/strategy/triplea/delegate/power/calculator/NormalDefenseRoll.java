package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;

/**
 * Calculates the value of a normal defensive dice roll
 *
 * <p>This takes into account territory effects, friendly support, and enemy support
 */
public class NormalDefenseRoll extends StrengthOrRollCalculator {

  NormalDefenseRoll(
      final AvailableSupportCalculator friendlySupport,
      final AvailableSupportCalculator enemySupport) {
    super(friendlySupport, enemySupport);
  }

  @Override
  public int getValue(final Unit unit) {
    final RollValue rollValue =
        RollValue.of(unit.getUnitAttachment().getDefenseRolls(unit.getOwner()))
            .add(addSupport(unit, friendlySupportTracker, UnitSupportAttachment::getRoll))
            .add(addSupport(unit, enemySupportTracker, UnitSupportAttachment::getRoll));
    return rollValue.minMax();
  }
}
