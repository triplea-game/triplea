package games.strategy.engine.framework.startup.ui.editors.validators;

/**
 * A validator that validates that a string is a number, and within a given min/max range
 * @author Klaus Groenbaek
 */
public class IntegerValidator implements IValidator
{
	//-----------------------------------------------------------------------
	// instance fields
	//-----------------------------------------------------------------------
	private int m_min;
	private int m_max;
	//-----------------------------------------------------------------------
	// constructors
	//-----------------------------------------------------------------------

	public IntegerValidator(int min	, int max)
	{
		m_min = min;
		m_max = max;
	}

	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------

	public boolean isValid(String text)
	{
		try
		{
			int i = Integer.parseInt(text);
			return m_min <= i && m_max >= i;

		} catch (NumberFormatException e)
		{
			return false;
		}
	}
}
