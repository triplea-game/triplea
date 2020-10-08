package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;

/**
 * Calculates the value of a normal offensive dice roll
 *
 * <p>This takes into account marine, bombarding, territory effects, friendly support, and enemy
 * support
 */
public class NormalOffenseRoll extends StrengthOrRollCalculator {

  NormalOffenseRoll(
      final AvailableSupportCalculator friendlySupport,
      final AvailableSupportCalculator enemySupport) {
    super(friendlySupport, enemySupport);
  }

  @Override
  public int getValue(final Unit unit) {
    final RollValue rollValue =
        RollValue.of(unit.getUnitAttachment().getAttackRolls(unit.getOwner()))
            .add(addSupport(unit, friendlySupportTracker, UnitSupportAttachment::getRoll))
            .add(addSupport(unit, enemySupportTracker, UnitSupportAttachment::getRoll));
    return rollValue.minMax();
  }
}
