package games.strategy.engine.framework.startup.ui.editors.validators;

/**
 * A validator that validates that the text is not empty
 */
public class NonEmptyValidator implements IValidator {
  @Override
  public boolean isValid(final String text) {
    return text.length() > 0;
  }
}
