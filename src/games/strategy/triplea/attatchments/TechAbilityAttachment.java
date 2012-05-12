package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
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
	
	// TODO: heavyBomber, must wait so that we can refactor the engine to handle multiple rolls for any kind of unit, etc.
	
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
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("rocketDiceNumber can not be empty or have more than two fields" + thisErrorMsg());
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
	
	//
	// Static Methods for interpreting data in attachments
	//
	public static int getAttackBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
	
	public static float getRepairDiscount(final PlayerID player, final GameData data)
	{
		float rVal = 1.0F;
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				final int min = taa.getRepairDiscount();
				if (min == -1)
					continue;
				else
				{
					float fmin = min;
					fmin = fmin / 100.0F;
					rVal -= fmin;
				}
			}
		}
		return Math.max(0.0F, rVal);
	}
	
	public static int getWarBondDiceSides(final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
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
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				airborneBases.addAll(taa.getAirborneBases());
			}
		}
		return airborneBases;
	}
	
	/**
	 * Must be done only in GameParser, and only after we have already parsed ALL technologies, attachments, and game options/properties.
	 * 
	 * @param data
	 * @throws GameParseException
	 */
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
			TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa == null)
			{
				// TODO: debating if we should have flags for things like "air", "land", "sea", "aaGun", "factory", "strategic bomber", etc.
				// perhaps just the easy ones, of air, land, and sea?
				if (ta.equals(TechAdvance.LONG_RANGE_AIRCRAFT))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allAir = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsAir);
					for (final UnitType air : allAir)
					{
						taa.setMovementBonus("2:" + air.getName());
					}
				}
				else if (ta.equals(TechAdvance.AA_RADAR))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allAA = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsAAforAnything);
					for (final UnitType aa : allAA)
					{
						taa.setRadarBonus("1:" + aa.getName());
					}
				}
				else if (ta.equals(TechAdvance.SUPER_SUBS))
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
				else if (ta.equals(TechAdvance.JET_POWER))
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
				else if (ta.equals(TechAdvance.INCREASED_FACTORY_PRODUCTION))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allFactories = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsFactoryOrCanProduceUnits);
					for (final UnitType factory : allFactories)
					{
						taa.setProductionBonus("2:" + factory.getName());
						taa.setMinimumTerritoryValueForProductionBonus("3");
						taa.setRepairDiscount("50"); // means a 50% discount, which is half price
					}
				}
				else if (ta.equals(TechAdvance.WAR_BONDS))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					taa.setWarBondDiceSides(Integer.toString(data.getDiceSides()));
					taa.setWarBondDiceNumber("1");
				}
				else if (ta.equals(TechAdvance.ROCKETS))
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
				else if (ta.equals(TechAdvance.DESTROYER_BOMBARD))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allDestroyers = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsDestroyer);
					for (final UnitType destroyer : allDestroyers)
					{
						taa.setUnitAbilitiesGained(destroyer.getName() + ":" + ABILITY_CAN_BOMBARD);
					}
				}
				/*else if (ta.equals(TechAdvance.HEAVY_BOMBER))
				{
					// TODO: heavyBomber, must wait so that we can refactor the engine to handle multiple rolls for any kind of unit, etc.
				}*/
				//
				// The following technologies should NOT have ability attachments for them:
				// shipyards and industrialTechnology = because it is better to use a Trigger to change player's production
				// improvedArtillerySupport = because it is already completely atomized and controlled through support attachments
				// paratroopers = because it is already completely atomized and controlled through unit attachments + game options
				// mechanizedInfantry = because it is already completely atomized and controlled through unit attachments
				// IF one of the above named techs changes what it does in a future version of a&a, and the change is large enough or different enough that it can not be done easily with a new game option,
				// then it is better to create a new tech rather than change the old one, and give the new one a new name, like paratroopers2 or paratroopersAttack, or some crap.
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
		// TODO Auto-generated method stub
	}
}
