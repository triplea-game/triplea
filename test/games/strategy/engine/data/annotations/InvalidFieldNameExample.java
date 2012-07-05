package games.strategy.engine.data.annotations;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;

/**
 * Class with an invalid field that doesn't match the setter annotated with @GameProperty
 * 
 * @author Klaus Groenbaek
 */
public class InvalidFieldNameExample extends DefaultAttachment
{
	private static final long serialVersionUID = 2902170223595163219L;
	
	protected InvalidFieldNameExample(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	private String attribute; // should have been prefixed with "m_". Should cause test to fail.
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	public String getAttribute()
	{
		return attribute;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttribute(final String attribute)
	{
		this.attribute = attribute;
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
