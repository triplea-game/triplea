package games.strategy.engine.framework.startup.ui.editors.validators;

/**
 * A validator that validates that the text is not empty
 * 
 * @author Klaus Groenbaek
 */
public class NonEmptyValidator implements IValidator
{
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	public boolean isValid(final String text)
	{
		return text.length() > 0;
	}
}
