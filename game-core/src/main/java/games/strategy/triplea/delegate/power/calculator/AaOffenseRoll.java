package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;

/**
 * Calculates the value of an aa/targeted offensive dice roll
 *
 * <p>This takes into account friendly support, and enemy support
 */
public class AaOffenseRoll extends StrengthOrRollCalculator {

  AaOffenseRoll(
      final AvailableSupportCalculator friendlySupport,
      final AvailableSupportCalculator enemySupport) {
    super(friendlySupport, enemySupport);
  }

  @Override
  public int getValue(final Unit unit) {
    final RollValue rollValue =
        RollValue.of(unit.getUnitAttachment().getMaxAaAttacks())
            .add(addSupport(unit, friendlySupportTracker, UnitSupportAttachment::getAaRoll))
            .add(addSupport(unit, enemySupportTracker, UnitSupportAttachment::getAaRoll));
    return rollValue.minMax();
  }
}
