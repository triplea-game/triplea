package games.strategy.triplea.delegate.power.calculator;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

/**
 * Holds the value of a dice roll.
 *
 * <p>Handles infinite values and correctly limits the value to above 0
 */
@Value(staticConstructor = "of")
@Getter(AccessLevel.NONE)
class RollValue {

  int value;
  boolean isInfinite;

  static RollValue of(final int value) {
    return RollValue.of(value, value == -1);
  }

  RollValue toValue(final int value) {
    return RollValue.of(value, false);
  }

  RollValue add(final int extraValue) {
    return isInfinite ? this : RollValue.of(value + extraValue, false);
  }

  int getValue() {
    // rolls don't have a maximum
    return isInfinite ? -1 : Math.max(0, value);
  }

  boolean isZero() {
    return value == 0;
  }
}
