package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.function.Predicate;

/**
 * Calculates the value of an aa/targeted defensive dice roll
 *
 * <p>This takes into account friendly support, and enemy support
 */
public class AaDefenseRoll extends StrengthOrRollCalculator {

  AaDefenseRoll(
      final AvailableSupportCalculator friendlySupport,
      final AvailableSupportCalculator enemySupport) {
    super(friendlySupport, enemySupport);
  }

  @Override
  public int getValue(final Unit unit) {
    final RollValue rollValue =
        RollValue.of(unit.getUnitAttachment().getMaxAaAttacks())
            .add(addSupport(unit, friendlySupportTracker))
            .add(addSupport(unit, enemySupportTracker));
    return rollValue.minMax();
  }

  @Override
  Predicate<UnitSupportAttachment> getRuleFilter() {
    return UnitSupportAttachment::getAaRoll;
  }
}
