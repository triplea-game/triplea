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
	
	private static final long serialVersionUID = -5820318094331518742L;
	private int m_techCost;
	private boolean m_heavyBomber;
	private String m_attribute;
	private IntegerMap<UnitType> m_givesMovement = new IntegerMap<UnitType>();
	
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
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTechCost(final String techCost)
	{
		m_techCost = getInt(techCost);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTechCost(final Integer techCost)
	{
		m_techCost = techCost;
	}
	
	public int getTechCost()
	{
		return m_techCost;
	}
	
	public void resetTechCost()
	{
		m_techCost = 5;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setHeavyBomber(final String heavyBomber)
	{
		m_heavyBomber = getBool(heavyBomber);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setHeavyBomber(final Boolean heavyBomber)
	{
		m_heavyBomber = heavyBomber;
	}
	
	public boolean getHeavyBomber()
	{
		return m_heavyBomber;
	}
	
	public void resetHeavyBomber()
	{
		m_heavyBomber = false;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttribute(final String attribute)
	{
		m_attribute = attribute;
	}
	
	public String getAttribute()
	{
		return m_attribute;
	}
	
	public void resetAttribute()
	{
		m_attribute = null;
	}
	
	@InternalDoNotExport
	public void setNotAProperty(final String notAProperty)
	{
		m_notAProperty = notAProperty;
	}
	
	public String getNotAProperty()
	{
		return m_notAProperty;
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
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setGivesMovement(final IntegerMap<UnitType> value)
	{
		m_givesMovement = value;
	}
	
	public IntegerMap<UnitType> getGivesMovement()
	{
		return m_givesMovement;
	}
	
	public void clearGivesMovement()
	{
		m_givesMovement.clear();
	}
	
	public void resetGivesMovement()
	{
		m_givesMovement = new IntegerMap<UnitType>();
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
	
}
