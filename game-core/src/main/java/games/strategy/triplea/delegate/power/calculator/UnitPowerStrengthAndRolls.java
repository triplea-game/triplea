package games.strategy.triplea.delegate.power.calculator;

import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

/** Stores and computes an individual unit's power, strength and roll */
@Value
@Getter(AccessLevel.PACKAGE)
@Builder(access = AccessLevel.PACKAGE, toBuilder = true)
public class UnitPowerStrengthAndRolls {
  @Getter(AccessLevel.PUBLIC)
  @Nonnull
  Integer power;

  @NonNull StrengthAndRolls strengthAndRolls;

  @NonNull PowerCalculator powerCalculator;
  @NonNull Boolean chooseBestRoll;
  @NonNull Integer diceSides;

  @Value(staticConstructor = "of")
  static class StrengthAndRolls {
    StrengthValue strength;
    RollValue rolls;

    private StrengthAndRolls(final StrengthValue strength, final RollValue rolls) {
      if (strength.isZero() || rolls.isZero()) {
        this.strength = strength.toValue(0);
        this.rolls = rolls.toValue(0);
      } else {
        this.strength = strength;
        this.rolls = rolls;
      }
    }
  }

  public int getStrength() {
    return strengthAndRolls.getStrength().getValue();
  }

  public int getRolls() {
    return strengthAndRolls.getRolls().getValue();
  }

  public UnitPowerStrengthAndRolls subtractStrength(final int strengthToSubtract) {
    return update(
        strengthAndRolls.getStrength().add(-1 * strengthToSubtract), strengthAndRolls.getRolls());
  }

  private UnitPowerStrengthAndRolls update(
      final StrengthValue newStrength, final RollValue newRolls) {
    return this.toBuilder()
        .strengthAndRolls(StrengthAndRolls.of(newStrength, newRolls))
        .power(powerCalculator.getValue(chooseBestRoll, diceSides, newStrength, newRolls))
        .build();
  }

  public UnitPowerStrengthAndRolls subtractRolls(final int rollsToSubtract) {
    return update(
        strengthAndRolls.getStrength(), strengthAndRolls.getRolls().add(-1 * rollsToSubtract));
  }

  UnitPowerStrengthAndRolls updateRolls(final int newRolls) {
    return update(strengthAndRolls.getStrength(), strengthAndRolls.getRolls().toValue(newRolls));
  }

  UnitPowerStrengthAndRolls toZero() {
    final StrengthValue newStrength = strengthAndRolls.getStrength().toValue(0);
    final RollValue newRolls = strengthAndRolls.getRolls().toValue(0);
    return this.toBuilder()
        .strengthAndRolls(StrengthAndRolls.of(newStrength, newRolls))
        .power(powerCalculator.getValue(chooseBestRoll, diceSides, newStrength, newRolls))
        .build();
  }
}
