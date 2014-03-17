package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Attaches to technologies.
 * Also contains static methods of interpreting data from all technology attachments that a player has.
 * 
 * @author veqryn (Mark Christopher Duncan)
 * 
 */
public class TechAbilityAttachment extends DefaultAttachment
{
	private static final long serialVersionUID = 1866305599625384294L;
	
	/**
	 * Convenience method.
	 */
	public static TechAbilityAttachment get(final TechAdvance type)
	{
		if (type instanceof GenericTechAdvance)
		{
			// generic techs can name a hardcoded tech, therefore if it exists we should use the hard coded tech's attachment.
			// (if the map maker doesn't want to use the hardcoded tech's attachment, they should not name a hardcoded tech)
			final TechAdvance hardCodedAdvance = ((GenericTechAdvance) type).getAdvance();
			if (hardCodedAdvance != null)
			{
				final TechAbilityAttachment hardCodedTechAttachment = (TechAbilityAttachment) hardCodedAdvance.getAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME);
				return hardCodedTechAttachment;
			}
		}
		final TechAbilityAttachment rVal = (TechAbilityAttachment) type.getAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME);
		return rVal;
	}
	
	/**
	 * Convenience method.
	 */
	public static TechAbilityAttachment get(final TechAdvance type, final String nameOfAttachment)
	{
		final TechAbilityAttachment rVal = (TechAbilityAttachment) type.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("No technology attachment for:" + type.getName() + " with name:" + nameOfAttachment);
		return rVal;
	}
	
	// unitAbilitiesGained Static Strings
	public static final String ABILITY_CAN_BLITZ = "canBlitz";
	public static final String ABILITY_CAN_BOMBARD = "canBombard";
	//
	// attachment fields
	//
	private IntegerMap<UnitType> m_attackBonus = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_defenseBonus = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_movementBonus = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_radarBonus = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_airAttackBonus = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_airDefenseBonus = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_productionBonus = new IntegerMap<UnitType>();
	private int m_minimumTerritoryValueForProductionBonus = -1; // -1 means not set
	private int m_repairDiscount = -1; // -1 means not set
	private int m_warBondDiceSides = -1; // -1 means not set
	private int m_warBondDiceNumber = 0;
	// private int m_rocketDiceSides = -1; // -1 means not set // not needed because this is controlled in the unit attachment with bombingBonus and bombingMaxDieSides
	private IntegerMap<UnitType> m_rocketDiceNumber = new IntegerMap<UnitType>();
	private int m_rocketDistance = 0;
	private int m_rocketNumberPerTerritory = 0;
	private HashMap<UnitType, HashSet<String>> m_unitAbilitiesGained = new HashMap<UnitType, HashSet<String>>();
	private boolean m_airborneForces = false;
	private IntegerMap<UnitType> m_airborneCapacity = new IntegerMap<UnitType>();
	private HashSet<UnitType> m_airborneTypes = new HashSet<UnitType>();
	private int m_airborneDistance = 0;
	private HashSet<UnitType> m_airborneBases = new HashSet<UnitType>();
	private HashMap<String, HashSet<UnitType>> m_airborneTargettedByAA = new HashMap<String, HashSet<UnitType>>();
	private IntegerMap<UnitType> m_attackRollsBonus = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_defenseRollsBonus = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_bombingBonus = new IntegerMap<UnitType>();
	
	//
	// constructor
	//
	public TechAbilityAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	//
	// setters and getters
	//
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setAttackBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("attackBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_attackBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackBonus(final IntegerMap<UnitType> value)
	{
		m_attackBonus = value;
	}
	
	public IntegerMap<UnitType> getAttackBonus()
	{
		return m_attackBonus;
	}
	
	public void clearAttackBonus()
	{
		m_attackBonus.clear();
	}
	
	public void resetAttackBonus()
	{
		m_attackBonus = new IntegerMap<UnitType>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setDefenseBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("defenseBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_defenseBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDefenseBonus(final IntegerMap<UnitType> value)
	{
		m_defenseBonus = value;
	}
	
	public IntegerMap<UnitType> getDefenseBonus()
	{
		return m_defenseBonus;
	}
	
	public void clearDefenseBonus()
	{
		m_defenseBonus.clear();
	}
	
	public void resetDefenseBonus()
	{
		m_defenseBonus = new IntegerMap<UnitType>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setMovementBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("movementBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_movementBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMovementBonus(final IntegerMap<UnitType> value)
	{
		m_movementBonus = value;
	}
	
	public IntegerMap<UnitType> getMovementBonus()
	{
		return m_movementBonus;
	}
	
	public void clearMovementBonus()
	{
		m_movementBonus.clear();
	}
	
	public void resetMovementBonus()
	{
		m_movementBonus = new IntegerMap<UnitType>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setRadarBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("radarBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_radarBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRadarBonus(final IntegerMap<UnitType> value)
	{
		m_radarBonus = value;
	}
	
	public IntegerMap<UnitType> getRadarBonus()
	{
		return m_radarBonus;
	}
	
	public void clearRadarBonus()
	{
		m_radarBonus.clear();
	}
	
	public void resetRadarBonus()
	{
		m_radarBonus = new IntegerMap<UnitType>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setAirAttackBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("airAttackBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_airAttackBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirAttackBonus(final IntegerMap<UnitType> value)
	{
		m_airAttackBonus = value;
	}
	
	public IntegerMap<UnitType> getAirAttackBonus()
	{
		return m_airAttackBonus;
	}
	
	public void clearAirAttackBonus()
	{
		m_airAttackBonus.clear();
	}
	
	public void resetAirAttackBonus()
	{
		m_airAttackBonus = new IntegerMap<UnitType>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setAirDefenseBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("airDefenseBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_airDefenseBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirDefenseBonus(final IntegerMap<UnitType> value)
	{
		m_airDefenseBonus = value;
	}
	
	public IntegerMap<UnitType> getAirDefenseBonus()
	{
		return m_airDefenseBonus;
	}
	
	public void clearAirDefenseBonus()
	{
		m_airDefenseBonus.clear();
	}
	
	public void resetAirDefenseBonus()
	{
		m_airDefenseBonus = new IntegerMap<UnitType>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setProductionBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("productionBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_productionBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setProductionBonus(final IntegerMap<UnitType> value)
	{
		m_productionBonus = value;
	}
	
	public IntegerMap<UnitType> getProductionBonus()
	{
		return m_productionBonus;
	}
	
	public void clearProductionBonus()
	{
		m_productionBonus.clear();
	}
	
	public void resetProductionBonus()
	{
		m_productionBonus = new IntegerMap<UnitType>();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMinimumTerritoryValueForProductionBonus(final String value) throws GameParseException
	{
		final int v = getInt(value);
		if ((v != -1) && (v < 0 || v > 10000))
			throw new GameParseException("minimumTerritoryValueForProductionBonus must be -1 (no effect), or be between 0 and 10000" + thisErrorMsg());
		m_minimumTerritoryValueForProductionBonus = v;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMinimumTerritoryValueForProductionBonus(final Integer value)
	{
		m_minimumTerritoryValueForProductionBonus = value;
	}
	
	public int getMinimumTerritoryValueForProductionBonus()
	{
		return m_minimumTerritoryValueForProductionBonus;
	}
	
	public void resetMinimumTerritoryValueForProductionBonus()
	{
		m_minimumTerritoryValueForProductionBonus = -1;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRepairDiscount(final String value) throws GameParseException
	{
		final int v = getInt(value);
		if ((v != -1) && (v < 0 || v > 100))
			throw new GameParseException("m_repairDiscount must be -1 (no effect), or be between 0 and 100" + thisErrorMsg());
		m_repairDiscount = v;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRepairDiscount(final Integer value)
	{
		m_repairDiscount = value;
	}
	
	public int getRepairDiscount()
	{
		return m_repairDiscount;
	}
	
	public void resetRepairDiscount()
	{
		m_repairDiscount = -1;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWarBondDiceSides(final String value) throws GameParseException
	{
		final int v = getInt(value);
		if ((v != -1) && (v < 0 || v > 200))
			throw new GameParseException("warBondDiceSides must be -1 (no effect), or be between 0 and 200" + thisErrorMsg());
		m_warBondDiceSides = v;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWarBondDiceSides(final Integer value)
	{
		m_warBondDiceSides = value;
	}
	
	public int getWarBondDiceSides()
	{
		return m_warBondDiceSides;
	}
	
	public void resetWarBondDiceSides()
	{
		m_warBondDiceSides = -1;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWarBondDiceNumber(final String value) throws GameParseException
	{
		final int v = getInt(value);
		if (v < 0 || v > 100)
			throw new GameParseException("warBondDiceNumber must be between 0 and 100" + thisErrorMsg());
		m_warBondDiceNumber = v;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWarBondDiceNumber(final Integer value)
	{
		m_warBondDiceNumber = value;
	}
	
	public int getWarBondDiceNumber()
	{
		return m_warBondDiceNumber;
	}
	
	public void resetWarBondDiceNumber()
	{
		m_warBondDiceNumber = 0;
	}
	
	/*@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRocketDiceSides(final String value) throws GameParseException
	{
		final int v = getInt(value);
		if ((v != -1) && (v < 0 || v > 200))
			throw new GameParseException("rocketDiceSides must be -1 (no effect), or be between 0 and 200" + thisErrorMsg());
		m_rocketDiceSides = v;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRocketDiceSides(final Integer value)
	{
		m_rocketDiceSides = value;
	}
	
	public int getRocketDiceSides()
	{
		return m_rocketDiceSides;
	}*/
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setRocketDiceNumber(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("rocketDiceNumber must have two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_rocketDiceNumber.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRocketDiceNumber(final IntegerMap<UnitType> value)
	{
		m_rocketDiceNumber = value;
	}
	
	public IntegerMap<UnitType> getRocketDiceNumber()
	{
		return m_rocketDiceNumber;
	}
	
	public void clearRocketDiceNumber()
	{
		m_rocketDiceNumber.clear();
	}
	
	public void resetRocketDiceNumber()
	{
		m_rocketDiceNumber = new IntegerMap<UnitType>();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRocketDistance(final String value) throws GameParseException
	{
		final int v = getInt(value);
		if (v < 0 || v > 100)
			throw new GameParseException("rocketDistance must be between 0 and 100" + thisErrorMsg());
		m_rocketDistance = v;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRocketDistance(final Integer value)
	{
		m_rocketDistance = value;
	}
	
	public int getRocketDistance()
	{
		return m_rocketDistance;
	}
	
	public void resetRocketDistance()
	{
		m_rocketDistance = 0;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRocketNumberPerTerritory(final String value) throws GameParseException
	{
		final int v = getInt(value);
		if (v < 0 || v > 200)
			throw new GameParseException("rocketNumberPerTerritory must be between 0 and 200" + thisErrorMsg());
		m_rocketNumberPerTerritory = v;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRocketNumberPerTerritory(final Integer value)
	{
		m_rocketNumberPerTerritory = value;
	}
	
	public int getRocketNumberPerTerritory()
	{
		return m_rocketNumberPerTerritory;
	}
	
	public void resetRocketNumberPerTerritory()
	{
		m_rocketNumberPerTerritory = 0;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setUnitAbilitiesGained(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length < 2)
			throw new GameParseException("unitAbilitiesGained must list the unit type, then all abilities gained" + thisErrorMsg());
		String unitType;
		unitType = s[0];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		HashSet<String> abilities = m_unitAbilitiesGained.get(ut);
		if (abilities == null)
			abilities = new HashSet<String>();
		// start at 1
		for (int i = 1; i < s.length; i++)
		{
			final String ability = s[i];
			if (!(ability.equals(ABILITY_CAN_BLITZ) || ability.equals(ABILITY_CAN_BOMBARD)))
				throw new GameParseException("unitAbilitiesGained so far only supports: " + ABILITY_CAN_BLITZ + " and " + ABILITY_CAN_BOMBARD + thisErrorMsg());
			abilities.add(ability);
		}
		m_unitAbilitiesGained.put(ut, abilities);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setUnitAbilitiesGained(final HashMap<UnitType, HashSet<String>> value)
	{
		m_unitAbilitiesGained = value;
	}
	
	public HashMap<UnitType, HashSet<String>> getUnitAbilitiesGained()
	{
		return m_unitAbilitiesGained;
	}
	
	public void clearUnitAbilitiesGained()
	{
		m_unitAbilitiesGained.clear();
	}
	
	public void resetUnitAbilitiesGained()
	{
		m_unitAbilitiesGained = new HashMap<UnitType, HashSet<String>>();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirborneForces(final String value) throws GameParseException
	{
		m_airborneForces = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirborneForces(final Boolean value)
	{
		m_airborneForces = value;
	}
	
	public boolean getAirborneForces()
	{
		return m_airborneForces;
	}
	
	public void resetAirborneForces()
	{
		m_airborneForces = false;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setAirborneCapacity(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("airborneCapacity can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_airborneCapacity.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirborneCapacity(final IntegerMap<UnitType> value)
	{
		m_airborneCapacity = value;
	}
	
	public IntegerMap<UnitType> getAirborneCapacity()
	{
		return m_airborneCapacity;
	}
	
	public void clearAirborneCapacity()
	{
		m_airborneCapacity.clear();
	}
	
	public void resetAirborneCapacity()
	{
		m_airborneCapacity = new IntegerMap<UnitType>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setAirborneTypes(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		for (final String u : s)
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(u);
			if (ut == null)
				throw new GameParseException("airborneTypes: no such unit type: " + u + thisErrorMsg());
			m_airborneTypes.add(ut);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirborneTypes(final HashSet<UnitType> value)
	{
		m_airborneTypes = value;
	}
	
	public HashSet<UnitType> getAirborneTypes()
	{
		return m_airborneTypes;
	}
	
	public void clearAirborneTypes()
	{
		m_airborneTypes.clear();
	}
	
	public void resetAirborneTypes()
	{
		m_airborneTypes = new HashSet<UnitType>();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirborneDistance(final String value) throws GameParseException
	{
		final int v = getInt(value);
		if (v < 0 || v > 100)
			throw new GameParseException("airborneDistance must be between 0 and 100" + thisErrorMsg());
		m_airborneDistance = v;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirborneDistance(final Integer value)
	{
		m_airborneDistance = value;
	}
	
	public int getAirborneDistance()
	{
		return m_airborneDistance;
	}
	
	public void resetAirborneDistance()
	{
		m_airborneDistance = 0;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setAirborneBases(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		for (final String u : s)
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(u);
			if (ut == null)
				throw new GameParseException("airborneBases: no such unit type: " + u + thisErrorMsg());
			m_airborneBases.add(ut);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirborneBases(final HashSet<UnitType> value)
	{
		m_airborneBases = value;
	}
	
	public HashSet<UnitType> getAirborneBases()
	{
		return m_airborneBases;
	}
	
	public void clearAirborneBases()
	{
		m_airborneBases.clear();
	}
	
	public void resetAirborneBases()
	{
		m_airborneBases = new HashSet<UnitType>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setAirborneTargettedByAA(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length < 2)
			throw new GameParseException("airborneTargettedByAA must have at least two fields" + thisErrorMsg());
		final String aaType = s[0];
		final HashSet<UnitType> unitTypes = new HashSet<UnitType>();
		for (int i = 1; i < s.length; i++)
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
			if (ut == null)
				throw new GameParseException("airborneTargettedByAA: no such unit type: " + s[i] + thisErrorMsg());
			unitTypes.add(ut);
		}
		m_airborneTargettedByAA.put(aaType, unitTypes);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirborneTargettedByAA(final HashMap<String, HashSet<UnitType>> value)
	{
		m_airborneTargettedByAA = value;
	}
	
	public HashMap<String, HashSet<UnitType>> getAirborneTargettedByAA()
	{
		return m_airborneTargettedByAA;
	}
	
	public void clearAirborneTargettedByAA()
	{
		m_airborneTargettedByAA.clear();
	}
	
	public void resetAirborneTargettedByAA()
	{
		m_airborneTargettedByAA = new HashMap<String, HashSet<UnitType>>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setAttackRollsBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("attackRollsBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_attackRollsBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackRollsBonus(final IntegerMap<UnitType> value)
	{
		m_attackRollsBonus = value;
	}
	
	public IntegerMap<UnitType> getAttackRollsBonus()
	{
		return m_attackRollsBonus;
	}
	
	public void clearAttackRollsBonus()
	{
		m_attackRollsBonus.clear();
	}
	
	public void resetAttackRollsBonus()
	{
		m_attackRollsBonus = new IntegerMap<UnitType>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setDefenseRollsBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("defenseRollsBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_defenseRollsBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDefenseRollsBonus(final IntegerMap<UnitType> value)
	{
		m_defenseRollsBonus = value;
	}
	
	public IntegerMap<UnitType> getDefenseRollsBonus()
	{
		return m_defenseRollsBonus;
	}
	
	public void clearDefenseRollsBonus()
	{
		m_defenseRollsBonus.clear();
	}
	
	public void resetDefenseRollsBonus()
	{
		m_defenseRollsBonus = new IntegerMap<UnitType>();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setBombingBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("bombingBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_bombingBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBombingBonus(final IntegerMap<UnitType> value)
	{
		m_bombingBonus = value;
	}
	
	public IntegerMap<UnitType> getBombingBonus()
	{
		return m_bombingBonus;
	}
	
	public void clearBombingBonus()
	{
		m_bombingBonus.clear();
	}
	
	public void resetBombingBonus()
	{
		m_bombingBonus = new IntegerMap<UnitType>();
	}
	
	//
	// Static Methods for interpreting data in attachments
	//
	public static int getAttackBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getAttackBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	public static int getDefenseBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getDefenseBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	public static int getMovementBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getMovementBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	public static int getRadarBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getRadarBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	public static int getAirAttackBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getAirAttackBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	public static int getAirDefenseBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getAirDefenseBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	public static int getProductionBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getProductionBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	public static int getMinimumTerritoryValueForProductionBonus(final PlayerID player, final GameData data)
	{
		int rVal = -1;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				final int min = taa.getMinimumTerritoryValueForProductionBonus();
				if (min == -1)
					continue;
				else if (rVal == -1 || min < rVal)
					rVal = min;
			}
		}
		return Math.max(0, rVal);
	}
	
	public static double getRepairDiscount(final PlayerID player, final GameData data)
	{
		double rVal = 1.0D;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				final int min = taa.getRepairDiscount();
				if (min == -1)
					continue;
				else
				{
					double fmin = min;
					fmin = fmin / 100.0F;
					rVal -= fmin;
				}
			}
		}
		return Math.max(0.0D, rVal);
	}
	
	public static int getWarBondDiceSides(final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				final int sides = taa.getWarBondDiceSides();
				if (sides > 0)
					rVal += sides;
			}
		}
		return Math.max(0, rVal);
	}
	
	public static int getWarBondDiceNumber(final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				final int number = taa.getWarBondDiceNumber();
				if (number > 0)
					rVal += number;
			}
		}
		return Math.max(0, rVal);
	}
	
	/*public static int getRocketDiceSides(final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				final int sides = taa.getRocketDiceSides();
				if (sides > 0)
					rVal += sides;
			}
		}
		return Math.max(0, rVal);
	}*/
	
	private static int getRocketDiceNumber(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getRocketDiceNumber().getInt(ut);
			}
		}
		return rVal;
	}
	
	public static int getRocketDiceNumber(final Collection<Unit> rockets, final GameData data)
	{
		int rVal = 0;
		for (final Unit u : rockets)
		{
			rVal += getRocketDiceNumber(u.getType(), u.getOwner(), data);
		}
		return rVal;
	}
	
	public static int getRocketDistance(final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				final int distance = taa.getRocketDistance();
				if (distance > 0)
					rVal += distance;
			}
		}
		return Math.max(0, rVal);
	}
	
	public static int getRocketNumberPerTerritory(final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				final int number = taa.getRocketNumberPerTerritory();
				if (number > 0)
					rVal += number;
			}
		}
		return Math.max(0, rVal);
	}
	
	private static HashSet<String> getUnitAbilitiesGained(final UnitType ut, final PlayerID player, final GameData data)
	{
		final HashSet<String> rVal = new HashSet<String>();
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				final HashSet<String> abilities = taa.getUnitAbilitiesGained().get(ut);
				if (abilities != null)
					rVal.addAll(abilities);
			}
		}
		return rVal;
	}
	
	public static boolean getUnitAbilitiesGained(final String filterForAbility, final UnitType ut, final PlayerID player, final GameData data)
	{
		final HashSet<String> abilities = getUnitAbilitiesGained(ut, player, data);
		if (abilities.contains(filterForAbility))
			return true;
		return false;
	}
	
	public static boolean getAllowAirborneForces(final PlayerID player, final GameData data)
	{
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				if (taa.getAirborneForces())
					return true;
			}
		}
		return false;
	}
	
	public static IntegerMap<UnitType> getAirborneCapacity(final PlayerID player, final GameData data)
	{
		final IntegerMap<UnitType> capacityMap = new IntegerMap<UnitType>();
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				capacityMap.add(taa.getAirborneCapacity());
			}
		}
		return capacityMap;
	}
	
	public static int getAirborneCapacity(final Collection<Unit> units, final PlayerID player, final GameData data)
	{
		final IntegerMap<UnitType> capacityMap = getAirborneCapacity(player, data);
		int rVal = 0;
		for (final Unit u : units)
		{
			rVal += Math.max(0, (capacityMap.getInt(u.getType()) - ((TripleAUnit) u).getLaunched()));
		}
		return rVal;
	}
	
	public static Set<UnitType> getAirborneTypes(final PlayerID player, final GameData data)
	{
		final Set<UnitType> airborneUnits = new HashSet<UnitType>();
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				airborneUnits.addAll(taa.getAirborneTypes());
			}
		}
		return airborneUnits;
	}
	
	public static int getAirborneDistance(final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getAirborneDistance();
			}
		}
		return Math.max(0, rVal);
	}
	
	public static Set<UnitType> getAirborneBases(final PlayerID player, final GameData data)
	{
		final Set<UnitType> airborneBases = new HashSet<UnitType>();
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				airborneBases.addAll(taa.getAirborneBases());
			}
		}
		return airborneBases;
	}
	
	public static HashMap<String, HashSet<UnitType>> getAirborneTargettedByAA(final PlayerID player, final GameData data)
	{
		final HashMap<String, HashSet<UnitType>> rVal = new HashMap<String, HashSet<UnitType>>();
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				final HashMap<String, HashSet<UnitType>> mapAA = taa.getAirborneTargettedByAA();
				if (mapAA != null && !mapAA.isEmpty())
				{
					for (final Entry<String, HashSet<UnitType>> entry : mapAA.entrySet())
					{
						HashSet<UnitType> current = rVal.get(entry.getKey());
						if (current == null)
							current = new HashSet<UnitType>();
						current.addAll(entry.getValue());
						rVal.put(entry.getKey(), current);
					}
				}
			}
		}
		return rVal;
	}
	
	public static int getAttackRollsBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getAttackRollsBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	public static int getDefenseRollsBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getDefenseRollsBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	public static int getBombingBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getBombingBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	/**
	 * Must be done only in GameParser, and only after we have already parsed ALL technologies, attachments, and game options/properties.
	 * 
	 * @param data
	 * @throws GameParseException
	 */
	@InternalDoNotExport
	public static void setDefaultTechnologyAttachments(final GameData data) throws GameParseException
	{
		// loop through all technologies. any "default/hard-coded" tech that doesn't have an attachment, will get its "default" attachment. any non-default tech are ignored.
		for (final TechAdvance techAdvance : TechAdvance.getTechAdvances(data))
		{
			final TechAdvance ta;
			if (techAdvance instanceof GenericTechAdvance)
			{
				final TechAdvance adv = ((GenericTechAdvance) techAdvance).getAdvance();
				if (adv != null)
					ta = adv;
				else
					continue;
			}
			else
			{
				ta = techAdvance;
			}
			final String propertyString = ta.getProperty();
			TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa == null)
			{
				// debating if we should have flags for things like "air", "land", "sea", "aaGun", "factory", "strategic bomber", etc.
				// perhaps just the easy ones, of air, land, and sea?
				if (propertyString.equals(TechAdvance.TECH_PROPERTY_LONG_RANGE_AIRCRAFT))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allAir = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsAir);
					for (final UnitType air : allAir)
					{
						taa.setMovementBonus("2:" + air.getName());
					}
				}
				else if (propertyString.equals(TechAdvance.TECH_PROPERTY_AA_RADAR))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allAA = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsAAforAnything);
					for (final UnitType aa : allAA)
					{
						taa.setRadarBonus("1:" + aa.getName());
					}
				}
				else if (propertyString.equals(TechAdvance.TECH_PROPERTY_SUPER_SUBS))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allSubs = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsSub);
					// final int defenseBonus = games.strategy.triplea.Properties.getSuper_Sub_Defense_Bonus(data);
					for (final UnitType sub : allSubs)
					{
						taa.setAttackBonus("1:" + sub.getName());
						// if (defenseBonus != 0)
						// taa.setDefenseBonus(defenseBonus + ":" + sub.getName());
					}
				}
				else if (propertyString.equals(TechAdvance.TECH_PROPERTY_JET_POWER))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allJets = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(),
								new CompositeMatchAnd<UnitType>(Matches.UnitTypeIsAir, Matches.UnitTypeIsStrategicBomber.invert()));
					final boolean ww2v3TechModel = games.strategy.triplea.Properties.getWW2V3TechModel(data);
					for (final UnitType jet : allJets)
					{
						if (ww2v3TechModel)
							taa.setAttackBonus("1:" + jet.getName());
						else
							taa.setDefenseBonus("1:" + jet.getName());
					}
				}
				else if (propertyString.equals(TechAdvance.TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allFactories = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeCanProduceUnits);
					for (final UnitType factory : allFactories)
					{
						taa.setProductionBonus("2:" + factory.getName());
						taa.setMinimumTerritoryValueForProductionBonus("3");
						taa.setRepairDiscount("50"); // means a 50% discount, which is half price
					}
				}
				else if (propertyString.equals(TechAdvance.TECH_PROPERTY_WAR_BONDS))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					taa.setWarBondDiceSides(Integer.toString(data.getDiceSides()));
					taa.setWarBondDiceNumber("1");
				}
				else if (propertyString.equals(TechAdvance.TECH_PROPERTY_ROCKETS))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allRockets = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsRocket);
					for (final UnitType rocket : allRockets)
					{
						taa.setRocketDiceNumber("1:" + rocket.getName());
					}
					// taa.setRocketDiceSides(Integer.toString(data.getDiceSides()));
					taa.setRocketDistance("3");
					taa.setRocketNumberPerTerritory("1");
				}
				else if (propertyString.equals(TechAdvance.TECH_PROPERTY_DESTROYER_BOMBARD))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allDestroyers = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsDestroyer);
					for (final UnitType destroyer : allDestroyers)
					{
						taa.setUnitAbilitiesGained(destroyer.getName() + ":" + ABILITY_CAN_BOMBARD);
					}
				}
				else if (propertyString.equals(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allBombers = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsStrategicBomber);
					final int heavyBomberDiceRollsTotal = games.strategy.triplea.Properties.getHeavyBomberDiceRolls(data);
					final boolean heavyBombersLHTR = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(data);
					for (final UnitType bomber : allBombers)
					{
						// TODO: The bomber dice rolls get set when the xml is parsed.
						final int heavyBomberDiceRollsBonus = heavyBomberDiceRollsTotal - UnitAttachment.get(bomber).getAttackRolls(PlayerID.NULL_PLAYERID); // we subtract the base rolls to get the bonus
						taa.setAttackRollsBonus(heavyBomberDiceRollsBonus + ":" + bomber.getName());
						if (heavyBombersLHTR)
						{
							// TODO: this all happens WHEN the xml is parsed. Which means if the user changes the game options, this does not get changed. (meaning, turning on LHTR bombers will not result in this bonus damage, etc. It would have to start on, in the xml.)
							taa.setDefenseRollsBonus(heavyBomberDiceRollsBonus + ":" + bomber.getName());
							taa.setBombingBonus("1:" + bomber.getName()); // LHTR adds 1 to base roll
						}
					}
				}
				//
				// The following technologies should NOT have ability attachments for them:
				// shipyards and industrialTechnology = because it is better to use a Trigger to change player's production
				// improvedArtillerySupport = because it is already completely atomized and controlled through support attachments
				// paratroopers = because it is already completely atomized and controlled through unit attachments + game options
				// mechanizedInfantry = because it is already completely atomized and controlled through unit attachments
				//
				// IF one of the above named techs changes what it does in a future version of a&a, and the change is large enough or different enough that it can not be done easily with a new game option,
				// then it is better to create a new tech rather than change the old one, and give the new one a new name, like paratroopers2 or paratroopersAttack or Airborne_Forces, or some crap.
				//
			}
		}
	}
	
	//
	// validator
	//
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		final TechAdvance ta = (TechAdvance) this.getAttachedTo();
		if (ta instanceof GenericTechAdvance)
		{
			final TechAdvance hardCodedAdvance = ((GenericTechAdvance) ta).getAdvance();
			if (hardCodedAdvance != null)
			{
				throw new GameParseException("A custom Generic Tech Advance naming a hardcoded tech, may not have a Tech Ability Attachment!" + this.thisErrorMsg());
			}
		}
	}
}
