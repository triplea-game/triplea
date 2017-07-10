package games.strategy.engine.framework.startup.ui.editors.validators;

/**
 * A simple interface for validating data from text fields.
 */
public interface IValidator {
  /**
   * Validates that the give input is valid.
   *
   * @param text
   *        the text to be validated
   * @return true if the data is valid
   */
  boolean isValid(String text);
}
