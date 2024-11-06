package games.strategy.engine.framework;

import games.strategy.triplea.formatter.MyFormatter;
import lombok.Getter;

/**
 * A collection of generated random numbers that have been verified as being untampered from a
 * remote random source.
 */
@Getter
public class VerifiedRandomNumbers {
  private final int[] values;
  private final String annotation;

  public VerifiedRandomNumbers(final String annotation, final int[] values) {
    this.values = values;
    this.annotation = annotation;
  }

  @Override
  public String toString() {
    return "Rolled : " + MyFormatter.asDice(values) + " for " + annotation;
  }
}
