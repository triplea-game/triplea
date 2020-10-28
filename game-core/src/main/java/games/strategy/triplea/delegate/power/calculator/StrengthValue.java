package games.strategy.triplea.delegate.power.calculator;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

/**
 * Holds the value of a dice strength
 *
 * <p>Correctly limits the value to between 0 and diceSides
 */
@Value(staticConstructor = "of")
@Getter(AccessLevel.NONE)
class StrengthValue {

  int diceSides;
  int value;

  StrengthValue add(final int extraValue) {
    return StrengthValue.of(diceSides, value + extraValue);
  }

  int getValue() {
    return Math.min(Math.max(value, 0), diceSides);
  }
}
