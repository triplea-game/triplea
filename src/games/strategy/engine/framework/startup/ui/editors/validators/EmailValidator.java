package games.strategy.engine.framework.startup.ui.editors.validators;

import games.strategy.util.Util;

/**
 * A validator which validates that a text string is an email
 * @author Klaus Groenbaek
 */
public class EmailValidator implements IValidator
{

	private boolean m_validIfEmpty;
	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------

	/**
	 * create a new instance
	 * @param validIfEmpty is the text valid if empty
	 */
	public EmailValidator(boolean validIfEmpty)
	{
		m_validIfEmpty = validIfEmpty;
	}

	public boolean isValid(String text)
	{
		if (text.length() == 0) {
			return m_validIfEmpty;
		}
		return Util.isMailValid(text);


	}
}
