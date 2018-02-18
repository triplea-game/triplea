package games.strategy.engine.framework;

import games.strategy.triplea.formatter.MyFormatter;

public class VerifiedRandomNumbers {
  private final int[] values;
  private final String annotation;

  public VerifiedRandomNumbers(final String annotation, final int[] values) {
    this.values = values;
    this.annotation = annotation;
  }

  @Override
  public String toString() {
    return "Rolled :" + MyFormatter.asDice(values) + " for " + annotation;
  }

  /**
   * @return Returns the annotation.
   */
  public String getAnnotation() {
    return annotation;
  }

  /**
   * @return Returns the values.
   */
  public int[] getValues() {
    return values;
  }
}
