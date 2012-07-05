package games.strategy.engine.data.annotations;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.UnitType;
import games.strategy.util.IntegerMap;

/**
 * Class with an invalidly named clear method
 * 
 * @author Veqryn [Mark Christopher Duncan]
 */
public class InvalidResetExample extends DefaultAttachment
{
	private static final long serialVersionUID = 113427104352979892L;
	
	protected InvalidResetExample(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	private IntegerMap<UnitType> m_givesMovement = new IntegerMap<UnitType>();
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setGivesMovement(final String value)
	{
	}
	
	public void resetGiveMovement() // badly named, should cause test to fail
	{
		m_givesMovement = new IntegerMap<UnitType>();
	}
	
	public void clearGivesMovement()
	{
		m_givesMovement.clear();
	}
	
	public IntegerMap<UnitType> getGivesMovement()
	{
		return m_givesMovement;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
	}
}
