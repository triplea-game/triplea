package games.strategy.triplea.delegate.power.calculator;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

/**
 * Holds the value of either a dice strength or dice roll.
 *
 * <p>Handles infinite values and correctly limits the value depending on its type.
 */
@Value(staticConstructor = "of")
@Getter(AccessLevel.NONE)
class RollValue {

  int value;
  boolean isInfinite;

  static RollValue of(final int value) {
    return RollValue.of(value, value == -1);
  }

  RollValue add(final int extraValue) {
    return isInfinite ? this : RollValue.of(value + extraValue, false);
  }

  int minMax() {
    // rolls don't have a maximum
    return isInfinite ? -1 : Math.max(0, value);
  }
}
