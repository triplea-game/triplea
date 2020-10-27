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
class AaOffenseCombatValue implements CombatValue {

  @Getter(onMethod = @__({@Override}))
  @NonNull
  GameData gameData;

  @NonNull AvailableSupports supportFromFriends;
  @NonNull AvailableSupports supportFromEnemies;

  @Override
  public StrengthOrRollCalculator getRoll() {
    return new AaOffenseRoll(supportFromFriends, supportFromEnemies);
  }

  @Override
  public StrengthOrRollCalculator getStrength() {
    return new AaOffenseStrength(gameData, supportFromFriends, supportFromEnemies);
  }

  @Override
  public boolean isDefending() {
    return false;
  }

  static class AaOffenseRoll extends StrengthOrRollCalculator {

    AaOffenseRoll(final AvailableSupports friendlySupport, final AvailableSupports enemySupport) {
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

  static class AaOffenseStrength extends StrengthOrRollCalculator {

    private final GameData gameData;

    AaOffenseStrength(
        final GameData gameData,
        final AvailableSupports friendlySupport,
        final AvailableSupports enemySupport) {
      super(friendlySupport, enemySupport);
      this.gameData = gameData;
    }

    @Override
    public int getValue(final Unit unit) {
      return StrengthValue.of(
              gameData.getDiceSides(),
              unit.getUnitAttachment().getOffensiveAttackAa(unit.getOwner()))
          .add(addSupport(unit, friendlySupportTracker))
          .add(addSupport(unit, enemySupportTracker))
          .getValue();
    }

    @Override
    protected Predicate<UnitSupportAttachment> getRuleFilter() {
      return UnitSupportAttachment::getAaStrength;
    }
  }
}
