package games.strategy.engine.framework;

import games.strategy.triplea.formatter.MyFormatter;

public class VerifiedRandomNumbers {
  private final int[] m_values;
  private final String m_annotation;

  public VerifiedRandomNumbers(final String annotation, final int[] values) {
    m_values = values;
    m_annotation = annotation;
  }

  @Override
  public String toString() {
    return "Rolled :" + MyFormatter.asDice(m_values) + " for " + m_annotation;
  }

  /**
   * @return Returns the m_annotation.
   */
  public String getAnnotation() {
    return m_annotation;
  }

  /**
   * @return Returns the m_values.
   */
  public int[] getValues() {
    return m_values;
  }
}
