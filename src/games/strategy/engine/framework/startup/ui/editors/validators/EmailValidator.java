package games.strategy.engine.framework.startup.ui.editors.validators;

import games.strategy.util.Util;

/**
 * A validator which validates that a text string is an email
 * @author Klaus Groenbaek
 */
public class EmailValidator implements IValidator
{

	private boolean m_mayBeEmpty;
	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------


	public EmailValidator(boolean mayBeEmpty)
	{
		m_mayBeEmpty = mayBeEmpty;
	}

	public boolean isValid(String text)
	{
		if (text.isEmpty() ) {
			return m_mayBeEmpty;
		}
		return Util.isMailValid(text);


	}
}
