package games.strategy.triplea.delegate.power.calculator;

import lombok.Value;

/**
 * Holds the value of either a dice strength or dice roll.
 *
 * <p>Handles infinite values and correctly limits the value depending on its type.
 */
@Value(staticConstructor = "of")
class StrengthValue {

  int diceSides;
  int value;

  StrengthValue add(final int extraValue) {
    return StrengthValue.of(diceSides, value + extraValue);
  }

  int minMax() {
    return Math.min(Math.max(value, 0), diceSides);
  }
}
