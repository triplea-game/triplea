package games.strategy.engine.data.annotations;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.UnitType;
import games.strategy.util.IntegerMap;

/**
 * 
 * Test attachment that demonstrates how @GameProperty is used
 * 
 * @author Klaus Groenbaek
 */
public class ExampleAttachment extends DefaultAttachment
{
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	
	private int m_techCost;
	private boolean m_heavyBomber;
	private String m_attribute;
	private final IntegerMap<UnitType> m_givesMovement = new IntegerMap<UnitType>();
	
	@InternalDoNotExport
	private String m_notAProperty = "str";
	
	// -----------------------------------------------------------------------
	// constructors
	// -----------------------------------------------------------------------
	
	public ExampleAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	public int getTechCost()
	{
		return m_techCost;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTechCost(final String techCost)
	{
		m_techCost = getInt(techCost);
	}
	
	public boolean getHeavyBomber()
	{
		return m_heavyBomber;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setHeavyBomber(final String heavyBomber)
	{
		m_heavyBomber = getBool(heavyBomber);
	}
	
	public String getAttribute()
	{
		return m_attribute;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttribute(final String attribute)
	{
		m_attribute = attribute;
	}
	
	public String getNotAProperty()
	{
		return m_notAProperty;
	}
	
	@InternalDoNotExport
	public void setNotAProperty(final String notAProperty)
	{
		m_notAProperty = notAProperty;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setGivesMovement(final String value)
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new IllegalStateException("Unit Attachments: givesMovement can not be empty or have more than two fields");
		String unitTypeToProduce;
		unitTypeToProduce = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
		if (ut == null)
			throw new IllegalStateException("Unit Attachments: No unit called:" + unitTypeToProduce);
		// we should allow positive and negative numbers, since you can give bonuses to units or take away a unit's movement
		final int n = getInt(s[0]);
		m_givesMovement.add(ut, n);
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
		// TODO Auto-generated method stub
	}
	
}
