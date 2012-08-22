package games.strategy.engine.data.annotations;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;

/**
 * Example that used @GameProperty and has a getter with an invalid return type
 * 
 * @author Klaus Groenbaek
 */
public class InvalidReturnTypeExample extends DefaultAttachment
{
	private static final long serialVersionUID = -4598237822854346073L;
	
	protected InvalidReturnTypeExample(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	@SuppressWarnings("unused")
	private String m_attribute;
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	public int getAttribute() // is not returning our variable, so should cause test to fail
	{
		return 1;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttribute(final String attribute)
	{
		m_attribute = attribute;
	}
	
	public void resetAttribute()
	{
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
