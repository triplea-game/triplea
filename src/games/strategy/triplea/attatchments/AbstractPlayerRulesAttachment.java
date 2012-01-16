package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

/**
 * The purpose of this class is to separate the Rules Attachment variables and methods that affect Players,
 * from the Rules Attachment things that are part of conditions and national objectives. <br>
 * In other words, things like m_placementAnyTerritory (allows placing in any territory without need of a factory),
 * or m_movementRestrictionTerritories (restricts movement to certain territories), would go in This class.
 * While things like m_alliedOwnershipTerritories (a conditions for testing ownership of territories,
 * or m_objectiveValue (the money given if the condition is true), would NOT go in This class. <br>
 * Please do not add new things to this class. Any new Player-Rules type of stuff should go in "PlayerAttachment".
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 */
public abstract class AbstractPlayerRulesAttachment extends AbstractRulesAttachment
{
	private static final long serialVersionUID = 7224407193725789143L;
	
	// Please do not add new things to this class. Any new Player-Rules type of stuff should go in "PlayerAttachment".
	// These variables are related to a "rulesAttatchment" that changes certain rules for the attached player. They are not related to conditions at all.
	protected String m_movementRestrictionType = null;
	protected String[] m_movementRestrictionTerritories;
	protected boolean m_placementAnyTerritory = false; // allows placing units in any owned land
	protected boolean m_placementAnySeaZone = false; // allows placing units in any sea by owned land
	protected boolean m_placementCapturedTerritory = false; // allows placing units in a captured territory
	protected boolean m_unlimitedProduction = false; // turns of the warning to the player when they produce more than they can place
	protected boolean m_placementInCapitalRestricted = false; // can only place units in the capital
	protected boolean m_dominatingFirstRoundAttack = false; // enemy units will defend at 1
	protected boolean m_negateDominatingFirstRoundAttack = false; // negates m_dominatingFirstRoundAttack
	protected final IntegerMap<UnitType> m_productionPerXTerritories = new IntegerMap<UnitType>(); // automatically produces 1 unit of a certain type per every X territories owned
	protected int m_placementPerTerritory = -1; // stops the user from placing units in any territory that already contains more than this number of owned units
	protected int m_maxPlacePerTerritory = -1; // maximum number of units that can be placed in each territory.
	
	// It would wreck most map xmls to move the rulesAttatchment's to another class, so don't move them out of here please!
	// However, any new rules attachments that are not conditions, should be put into the "PlayerAttachment" class.
	
	public AbstractPlayerRulesAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	/**
	 * Convenience method, will not return objectives and conditions, only the RulesAttachment (like what China in ww2v3 has).
	 * These attachments returned are not conditions to be tested, they are special rules affecting a player
	 * (for example: being able to produce without factories, or not being able to move out of specific territories).
	 * 
	 * @param player
	 *            PlayerID
	 * @return new rule attachment
	 */
	public static RulesAttachment get(final PlayerID player)
	{
		final RulesAttachment rVal = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (rVal == null)
			throw new IllegalStateException("Rules & Conditions: No rule attachment for:" + player.getName());
		return rVal;
	}
	
	public void setMovementRestrictionTerritories(final String value)
	{
		m_movementRestrictionTerritories = value.split(":");
		validateNames(m_movementRestrictionTerritories);
	}
	
	public String[] getMovementRestrictionTerritories()
	{
		return m_movementRestrictionTerritories;
	}
	
	public void setMovementRestrictionType(final String value) throws GameParseException
	{
		if (!(value.equals("disallowed") || value.equals("allowed")))
			throw new GameParseException("RulesAttachment: movementRestrictionType must be allowed or disallowed");
		m_movementRestrictionType = value;
	}
	
	public String getMovementRestrictionType()
	{
		return m_movementRestrictionType;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	public void setProductionPerXTerritories(final String value)
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new IllegalStateException("Rules Attachments: productionPerXTerritories can not be empty or have more than two fields");
		String unitTypeToProduce;
		if (s.length == 1)
			unitTypeToProduce = Constants.INFANTRY_TYPE;
		else
			unitTypeToProduce = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
		if (ut == null)
			throw new IllegalStateException("Rules Attachments: No unit called: " + unitTypeToProduce);
		final int n = getInt(s[0]);
		if (n <= 0)
			throw new IllegalStateException("Rules Attachments: productionPerXTerritories must be a positive integer");
		m_productionPerXTerritories.put(ut, n);
	}
	
	public IntegerMap<UnitType> getProductionPerXTerritories()
	{
		return m_productionPerXTerritories;
	}
	
	public void clearProductionPerXTerritories()
	{
		m_productionPerXTerritories.clear();
	}
	
	public void setPlacementPerTerritory(final String value)
	{
		m_placementPerTerritory = getInt(value);
	}
	
	public int getPlacementPerTerritory()
	{
		return m_placementPerTerritory;
	}
	
	public void setMaxPlacePerTerritory(final String value)
	{
		m_maxPlacePerTerritory = getInt(value);
	}
	
	public int getMaxPlacePerTerritory()
	{
		return m_maxPlacePerTerritory;
	}
	
	public void setPlacementAnyTerritory(final String value)
	{
		m_placementAnyTerritory = getBool(value);
	}
	
	public boolean getPlacementAnyTerritory()
	{
		return m_placementAnyTerritory;
	}
	
	public void setPlacementAnySeaZone(final String value)
	{
		m_placementAnySeaZone = getBool(value);
	}
	
	public boolean getPlacementAnySeaZone()
	{
		return m_placementAnySeaZone;
	}
	
	public void setPlacementCapturedTerritory(final String value)
	{
		m_placementCapturedTerritory = getBool(value);
	}
	
	public boolean getPlacementCapturedTerritory()
	{
		return m_placementCapturedTerritory;
	}
	
	public void setPlacementInCapitalRestricted(final String value)
	{
		m_placementInCapitalRestricted = getBool(value);
	}
	
	public boolean getPlacementInCapitalRestricted()
	{
		return m_placementInCapitalRestricted;
	}
	
	public void setUnlimitedProduction(final String value)
	{
		m_unlimitedProduction = getBool(value);
	}
	
	public boolean getUnlimitedProduction()
	{
		return m_unlimitedProduction;
	}
	
	public void setDominatingFirstRoundAttack(final String value)
	{
		m_dominatingFirstRoundAttack = getBool(value);
	}
	
	public boolean getDominatingFirstRoundAttack()
	{
		return m_dominatingFirstRoundAttack;
	}
	
	public void setNegateDominatingFirstRoundAttack(final String value)
	{
		m_negateDominatingFirstRoundAttack = getBool(value);
	}
	
	public boolean getNegateDominatingFirstRoundAttack()
	{
		return m_negateDominatingFirstRoundAttack;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		super.validate(data);
		validateNames(m_movementRestrictionTerritories);
	}
}
