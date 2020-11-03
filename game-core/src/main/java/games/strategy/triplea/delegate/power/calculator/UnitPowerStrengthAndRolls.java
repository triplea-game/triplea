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

  @NonNull Integer diceSides;

  @NonNull Boolean chooseBestRoll;

  public UnitPowerStrengthAndRolls subtractStrength(final int strengthToSubtract) {
    final int newStrength = Math.max(0, strength - strengthToSubtract);
    return this.toBuilder().strength(newStrength).rolls(newStrength == 0 ? 0 : rolls).build();
  }

  public UnitPowerStrengthAndRolls subtractRolls(final int rollsToSubtract) {
    final int newRolls = Math.max(0, rolls - rollsToSubtract);
    return this.toBuilder().strength(newRolls == 0 ? 0 : strength).rolls(newRolls).build();
  }

  /**
   * Calculates the power of the unit
   *
   * <p>Be careful using this with AA units because other AA units affect each other's power. Before
   * calling this on an AA unit, the entire group needs to be taken into account and adjusted. See
   * {@link AaPowerStrengthAndRolls} and how it adjusts the strength and rolls of a group of AA
   * units.
   */
  public int calculatePower() {
    int unitStrength = getStrength();
    final int unitRolls = getRolls();
    if (unitStrength <= 0 || unitRolls <= 0) {
      return 0;
    }
    // Bonus is normally 1 for most games
    final int extraRollBonus = Math.max(1, diceSides / 6);

    int totalPower = 0;
    if (unitRolls == 1) {
      totalPower += unitStrength;
    } else {
      if (chooseBestRoll) {
        // chooseBestRoll doesn't really make sense in LL. So instead,
        // we will just add +1 onto the power to simulate the gains of having the best die picked.
        unitStrength += extraRollBonus * (unitRolls - 1);
        totalPower += Math.min(unitStrength, diceSides);
      } else {
        totalPower += unitRolls * unitStrength;
      }
    }
    return totalPower;
  }
}
