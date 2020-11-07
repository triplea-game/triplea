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
  Integer strength;

  @Getter(AccessLevel.PUBLIC)
  @Nonnull
  Integer rolls;

  @Getter(AccessLevel.PUBLIC)
  @Nonnull
  Integer power;

  @NonNull PowerCalculator powerCalculator;
  @NonNull Boolean chooseBestRoll;
  @NonNull Integer diceSides;

  public UnitPowerStrengthAndRolls subtractStrength(final int strengthToSubtract) {
    final int newStrength = Math.max(0, strength - strengthToSubtract);
    return updateStrength(newStrength);
  }

  UnitPowerStrengthAndRolls updateStrength(final int newStrength) {
    final int newRolls = newStrength == 0 ? 0 : rolls;
    return update(newStrength, newRolls);
  }

  private UnitPowerStrengthAndRolls update(final int newStrength, final int newRolls) {
    return this.toBuilder()
        .strength(newStrength)
        .rolls(newRolls)
        .power(powerCalculator.getValue(chooseBestRoll, diceSides, newStrength, newRolls))
        .build();
  }

  public UnitPowerStrengthAndRolls subtractRolls(final int rollsToSubtract) {
    final int newRolls = Math.max(0, rolls - rollsToSubtract);
    return updateRolls(newRolls);
  }

  UnitPowerStrengthAndRolls updateRolls(final int newRolls) {
    final int newStrength = newRolls == 0 ? 0 : strength;
    return update(newStrength, newRolls);
  }
}
