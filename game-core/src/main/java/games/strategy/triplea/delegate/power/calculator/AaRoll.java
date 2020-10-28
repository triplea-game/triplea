package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.function.Predicate;

class AaRoll extends StrengthAndRollCalculator {

  AaRoll(final AvailableSupports friendlySupport, final AvailableSupports enemySupport) {
    super(friendlySupport, enemySupport);
  }

  @Override
  public int getValue(final Unit unit) {
    return RollValue.of(unit.getUnitAttachment().getMaxAaAttacks())
        .add(addSupport(unit, friendlySupportTracker))
        .add(addSupport(unit, enemySupportTracker))
        .getValue();
  }

  @Override
  protected Predicate<UnitSupportAttachment> getRuleFilter() {
    return UnitSupportAttachment::getAaRoll;
  }
}
