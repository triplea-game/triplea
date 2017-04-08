package games.strategy.engine.framework.startup.ui.editors.validators;

/**
 * A validator that validates that a string is a integer, and within a given min/max range.
 */
public class IntegerRangeValidator implements IValidator {
  private final int m_min;
  private final int m_max;

  /**
   * create a new instance.
   *
   * @param min
   *        the minimal value
   * @param max
   *        the maximal value
   */
  public IntegerRangeValidator(final int min, final int max) {
    m_min = min;
    m_max = max;
  }

  @Override
  public boolean isValid(final String text) {
    try {
      final int i = Integer.parseInt(text);
      return m_min <= i && m_max >= i;
    } catch (final NumberFormatException e) {
      return false;
    }
  }
}
