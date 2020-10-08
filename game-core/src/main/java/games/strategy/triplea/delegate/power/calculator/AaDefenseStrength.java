package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;

/**
 * Calculates the value of an aa/targeted defensive dice roll
 *
 * <p>This takes into account friendly support, and enemy support
 */
public class AaDefenseStrength extends StrengthOrRollCalculator {

  private final GameData gameData;

  AaDefenseStrength(
      final GameData gameData,
      final AvailableSupportCalculator friendlySupport,
      final AvailableSupportCalculator enemySupport) {
    super(friendlySupport, enemySupport);
    this.gameData = gameData;
  }

  @Override
  public int getValue(final Unit unit) {
    final StrengthValue strengthValue =
        StrengthValue.of(
                gameData.getDiceSides(), unit.getUnitAttachment().getAttackAa(unit.getOwner()))
            .add(addSupport(unit, friendlySupportTracker, UnitSupportAttachment::getAaStrength))
            .add(addSupport(unit, enemySupportTracker, UnitSupportAttachment::getAaStrength));
    return strengthValue.minMax();
  }
}
