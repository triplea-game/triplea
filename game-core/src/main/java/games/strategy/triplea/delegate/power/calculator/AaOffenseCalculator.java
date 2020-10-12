package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

/**
 * Calculates offense strength and roll for AA/Targeted dice
 *
 * <p>This takes into account friendly support, and enemy support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
class AaOffenseCalculator implements OffenseOrDefenseCalculator {

  @NonNull GameData data;
  @NonNull AvailableSupportTracker friendlySupportTracker;
  @NonNull AvailableSupportTracker enemySupportTracker;

  @Override
  public StrengthOrRollCalculator getRoll() {
    return new AaOffenseRoll(friendlySupportTracker, enemySupportTracker);
  }

  @Override
  public StrengthOrRollCalculator getStrength() {
    return new AaOffenseStrength(data, friendlySupportTracker, enemySupportTracker);
  }

  @Override
  public boolean isDefending() {
    return false;
  }

  @Override
  public GameData getGameData() {
    return data;
  }

  static class AaOffenseRoll extends StrengthOrRollCalculator {

    AaOffenseRoll(
        final AvailableSupportTracker friendlySupport, final AvailableSupportTracker enemySupport) {
      super(friendlySupport, enemySupport);
    }

    @Override
    public int getValue(final Unit unit) {
      final RollValue rollValue =
          RollValue.of(unit.getUnitAttachment().getMaxAaAttacks())
              .add(addSupport(unit, friendlySupportTracker))
              .add(addSupport(unit, enemySupportTracker));
      return rollValue.getValue();
    }

    @Override
    protected Predicate<UnitSupportAttachment> getRuleFilter() {
      return UnitSupportAttachment::getAaRoll;
    }
  }

  static class AaOffenseStrength extends StrengthOrRollCalculator {

    private final GameData gameData;

    AaOffenseStrength(
        final GameData gameData,
        final AvailableSupportTracker friendlySupport,
        final AvailableSupportTracker enemySupport) {
      super(friendlySupport, enemySupport);
      this.gameData = gameData;
    }

    @Override
    public int getValue(final Unit unit) {
      final StrengthValue strengthValue =
          StrengthValue.of(
                  gameData.getDiceSides(),
                  unit.getUnitAttachment().getOffensiveAttackAa(unit.getOwner()))
              .add(addSupport(unit, friendlySupportTracker))
              .add(addSupport(unit, enemySupportTracker));
      return strengthValue.getValue();
    }

    @Override
    protected Predicate<UnitSupportAttachment> getRuleFilter() {
      return UnitSupportAttachment::getAaStrength;
    }
  }
}
