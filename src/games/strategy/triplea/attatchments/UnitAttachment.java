/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * UnitAttachment.java
 * 
 * Created on November 8, 2001, 1:35 PM
 */
package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Despite the misleading name, this attaches not to individual Units but to UnitTypes.
 * 
 * Please follow this naming convention:
 * if the property is called "m_fooBar"
 * then you must have a "setFooBar" and "getFooBar",
 * and if the set method adds to a list or map, then you also need a "clearFooBar".
 * Do not change the name fooBar to make it plural or any other crap.
 * 
 * @author Sean Bridges and Mark Christopher Duncan
 * @version 1.0
 */
public class UnitAttachment extends DefaultAttachment
{
	private static final long serialVersionUID = -2946748686268541820L;
	
	/**
	 * Convenience method.
	 */
	public static UnitAttachment get(final UnitType type)
	{
		final UnitAttachment rVal = (UnitAttachment) type.getAttachment(Constants.UNIT_ATTACHMENT_NAME);
		if (rVal == null)
			throw new IllegalStateException("No unit type attachment for:" + type.getName());
		return rVal;
	}
	
	public static UnitAttachment get(final UnitType type, final String nameOfAttachment)
	{
		final UnitAttachment rVal = (UnitAttachment) type.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("No unit type attachment for:" + type.getName() + " with name:" + nameOfAttachment);
		return rVal;
	}
	
	public static Collection<UnitType> getUnitTypesFromUnitList(final Collection<Unit> units)
	{
		final Collection<UnitType> types = new ArrayList<UnitType>();
		for (final Unit u : units)
		{
			if (!types.contains(u.getType()))
				types.add(u.getType());
		}
		return types;
	}
	
	public static final String UNITSMAYNOTLANDONCARRIER = "unitsMayNotLandOnCarrier";
	public static final String UNITSMAYNOTLEAVEALLIEDCARRIER = "unitsMayNotLeaveAlliedCarrier";
	
	// movement related
	private boolean m_isAir = false;
	private boolean m_isSea = false;
	private int m_movement = 0;
	private boolean m_canBlitz = false;
	private boolean m_isKamikaze = false;
	private String[] m_canInvadeOnlyFrom; // a colon delimited list of transports where this unit may invade from, it supports "none" and if empty it allows you to invade from all
	private IntegerMap<Resource> m_fuelCost = new IntegerMap<Resource>();
	private boolean m_canNotMoveDuringCombatMove = false;
	private Tuple<Integer, String> m_movementLimit = null;
	
	// combat related
	private int m_attack = 0;
	private int m_defense = 0;
	private boolean m_isInfrastructure = false;
	private boolean m_canBombard = false;
	private int m_bombard = -1;
	private boolean m_isSub = false;
	private boolean m_isDestroyer = false;
	private boolean m_artillery = false;
	private boolean m_artillerySupportable = false;
	private int m_unitSupportCount = -1;
	private boolean m_isMarine = false;
	private boolean m_isSuicide = false;
	private Tuple<Integer, String> m_attackingLimit = null;
	private int m_attackRolls = 1;
	private int m_defenseRolls = 1;
	private boolean m_chooseBestRoll = false;
	
	// transportation related
	private boolean m_isCombatTransport = false;
	private int m_transportCapacity = -1; // -1 if cant transport
	private int m_transportCost = -1; // -1 if cant be transported
	private int m_carrierCapacity = -1; // -1 if cant act as a carrier
	private int m_carrierCost = -1; // -1 if cant land on a carrier
	private boolean m_isAirTransport = false;
	private boolean m_isAirTransportable = false;
	private boolean m_isInfantry = false;
	private boolean m_isLandTransport = false;
	
	// aa related
	// "isAA" and "isAAmovement" are also valid setters, used as shortcuts for calling multiple aa related setters. Must keep.
	private boolean m_isAAforCombatOnly = false;
	private boolean m_isAAforBombingThisUnitOnly = false;
	private boolean m_isAAforFlyOverOnly = false;
	private boolean m_isRocket = false;
	private int m_attackAA = 1;
	private int m_attackAAmaxDieSides = -1;
	private int m_maxAAattacks = -1; // -1 means infinite
	private String m_typeAA = "AA"; // default value for when it is not set
	private HashSet<UnitType> m_targetsAA = null; // null means targeting air units only
	private boolean m_mayOverStackAA = false; // if false, we can not shoot more times than there are number of planes
	private HashSet<UnitType> m_willNotFireIfPresent = new HashSet<UnitType>(); // if these enemy units are present, the gun does not fire at all
	
	// strategic bombing related
	private boolean m_isStrategicBomber = false;
	private int m_bombingMaxDieSides = -1;
	private int m_bombingBonus = -1;
	private boolean m_canIntercept = false;
	private boolean m_canEscort = false;
	private int m_airDefense = 0;
	private int m_airAttack = 0;
	private HashSet<UnitType> m_bombingTargets = null; // null means they can target any unit that can be damaged
	
	// production related
	private boolean m_isFactory = false;
	private boolean m_canProduceUnits = false;
	private int m_canProduceXUnits = -1; // -1 means either it can't produce any, or it produces at the value of the territory it is located in
	private IntegerMap<UnitType> m_createsUnitsList = new IntegerMap<UnitType>();
	private IntegerMap<Resource> m_createsResourcesList = new IntegerMap<Resource>();
	
	// damage related
	private boolean m_isTwoHit = false;
	private boolean m_canBeDamaged = false;
	private int m_maxDamage = -1; // -1 if can't be damaged
	private int m_maxOperationalDamage = -1; // -1 if can't be disabled
	private boolean m_canDieFromReachingMaxDamage = false;
	
	// placement related
	private boolean m_isConstruction = false;
	private String m_constructionType = "none"; // can be any String except for "none" if isConstruction is true
	private int m_constructionsPerTerrPerTypePerTurn = -1; // -1 if not set, is meaningless
	private int m_maxConstructionsPerTypePerTerr = -1; // -1 if not set, is meaningless
	private int m_canOnlyBePlacedInTerritoryValuedAtX = -1; // -1 means anywhere
	private ArrayList<String[]> m_requiresUnits = new ArrayList<String[]>(); // multiple colon delimited lists of the unit combos required for this unit to be built somewhere. (units must be in same territory, owned by player, not be disabled)
	private IntegerMap<UnitType> m_consumesUnits = new IntegerMap<UnitType>();
	private String[] m_unitPlacementRestrictions; // a colon delimited list of territories where this unit may not be placed
	// also an allowed setter is "setUnitPlacementOnlyAllowedIn", which just creates m_unitPlacementRestrictions with an inverted list of territories
	private int m_maxBuiltPerPlayer = -1; // -1 if infinite (infinite is default)
	private Tuple<Integer, String> m_placementLimit = null;
	
	// scrambling related
	private boolean m_canScramble = false;
	private boolean m_isAirBase = false;
	private int m_maxScrambleDistance = -1; // -1 if can't scramble
	private int m_maxScrambleCount = -1; // -1 for infinite
	
	// special abilities
	private int m_blockade = 0;
	private String[] m_repairsUnits; // a colon delimited list of the units this unit can repair. (units must be in same territory, unless this unit is land and the repaired unit is sea)
	private IntegerMap<UnitType> m_givesMovement = new IntegerMap<UnitType>();
	private ArrayList<Tuple<String, PlayerID>> m_destroyedWhenCapturedBy = new ArrayList<Tuple<String, PlayerID>>();
	// also an allowed setter is "setDestroyedWhenCapturedFrom" which will just create m_destroyedWhenCapturedBy with a specific list
	private LinkedHashMap<String, Tuple<String, IntegerMap<UnitType>>> m_whenCapturedChangesInto = new LinkedHashMap<String, Tuple<String, IntegerMap<UnitType>>>();
	private ArrayList<PlayerID> m_canBeCapturedOnEnteringBy = new ArrayList<PlayerID>();
	private ArrayList<PlayerID> m_canBeGivenByTerritoryTo = new ArrayList<PlayerID>();
	private ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> m_whenCombatDamaged = new ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>>(); // a set of information for dealing with special abilities or loss of abilities when a unit takes x-y amount of damage
	private ArrayList<String> m_receivesAbilityWhenWith = new ArrayList<String>(); // a kind of support attachment for giving actual unit attachment abilities or other to a unit, when in the precense or on the same route with another unit
	private HashSet<String> m_special = new HashSet<String>(); // currently used for: placement in original territories only,
	
	/** Creates new UnitAttachment */
	public UnitAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanIntercept(final String value)
	{
		m_canIntercept = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanIntercept(final Boolean value)
	{
		m_canIntercept = value;
	}
	
	public boolean getCanIntercept()
	{
		return m_canIntercept;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanEscort(final String value)
	{
		m_canEscort = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanEscort(final Boolean value)
	{
		m_canEscort = value;
	}
	
	public boolean getCanEscort()
	{
		return m_canEscort;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirDefense(final String value)
	{
		m_airDefense = getInt(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirDefense(final Integer value)
	{
		m_airDefense = value;
	}
	
	public int getAirDefense(final PlayerID player)
	{
		return (Math.min(getData().getDiceSides(), Math.max(0, m_airDefense + TechAbilityAttachment.getAirDefenseBonus((UnitType) this.getAttachedTo(), player, getData()))));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirAttack(final String value)
	{
		m_airAttack = getInt(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirAttack(final Integer value)
	{
		m_airAttack = value;
	}
	
	public int getAirAttack(final PlayerID player)
	{
		return (Math.min(getData().getDiceSides(), Math.max(0, m_airAttack + TechAbilityAttachment.getAirAttackBonus((UnitType) this.getAttachedTo(), player, getData()))));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAirTransport(final String s)
	{
		m_isAirTransport = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAirTransport(final Boolean s)
	{
		m_isAirTransport = s;
	}
	
	public boolean getIsAirTransport()
	{
		return m_isAirTransport;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAirTransportable(final String s)
	{
		m_isAirTransportable = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAirTransportable(final Boolean s)
	{
		m_isAirTransportable = s;
	}
	
	public boolean getIsAirTransportable()
	{
		return m_isAirTransportable;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setCanBeGivenByTerritoryTo(final String value) throws GameParseException
	{
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_canBeGivenByTerritoryTo.add(tempPlayer);
			else if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false"))
				m_canBeGivenByTerritoryTo.clear();
			else
				throw new GameParseException("No player named: " + name + thisErrorMsg());
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanBeGivenByTerritoryTo(final ArrayList<PlayerID> value)
	{
		m_canBeGivenByTerritoryTo = value;
	}
	
	public ArrayList<PlayerID> getCanBeGivenByTerritoryTo()
	{
		return m_canBeGivenByTerritoryTo;
	}
	
	public void clearCanBeGivenByTerritoryTo()
	{
		m_canBeGivenByTerritoryTo.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setCanBeCapturedOnEnteringBy(final String value) throws GameParseException
	{
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_canBeCapturedOnEnteringBy.add(tempPlayer);
			else
				throw new GameParseException("No player named: " + name + thisErrorMsg());
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanBeCapturedOnEnteringBy(final ArrayList<PlayerID> value)
	{
		m_canBeCapturedOnEnteringBy = value;
	}
	
	public ArrayList<PlayerID> getCanBeCapturedOnEnteringBy()
	{
		return m_canBeCapturedOnEnteringBy;
	}
	
	public void clearCanBeCapturedOnEnteringBy()
	{
		m_canBeCapturedOnEnteringBy.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setWhenCapturedChangesInto(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length < 5 || (s.length - 1) % 2 != 0)
			throw new GameParseException(
						"whenCapturedChangesInto must have 5 or more values, "
									+ "playerFrom:playerTo:keepAttributes:unitType:howMany (you may have additional unitType:howMany:unitType:howMany, etc"
									+ thisErrorMsg());
		final PlayerID pfrom = getData().getPlayerList().getPlayerID(s[0]);
		if (pfrom == null && !s[0].equals("any"))
			throw new GameParseException("whenCapturedChangesInto: No player named: " + s[0] + thisErrorMsg());
		final PlayerID pto = getData().getPlayerList().getPlayerID(s[1]);
		if (pto == null && !s[1].equals("any"))
			throw new GameParseException("whenCapturedChangesInto: No player named: " + s[1] + thisErrorMsg());
		getBool(s[2]);
		final IntegerMap<UnitType> unitsToMake = new IntegerMap<UnitType>();
		for (int i = 3; i < s.length; i++)
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
			if (ut == null)
				throw new GameParseException("whenCapturedChangesInto: No unit named: " + s[3] + thisErrorMsg());
			i++;
			final int howMany = getInt(s[i]);
			unitsToMake.put(ut, howMany);
		}
		m_whenCapturedChangesInto.put(s[0] + ":" + s[1], new Tuple<String, IntegerMap<UnitType>>(s[2], unitsToMake));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWhenCapturedChangesInto(final LinkedHashMap<String, Tuple<String, IntegerMap<UnitType>>> value)
	{
		m_whenCapturedChangesInto = value;
	}
	
	public LinkedHashMap<String, Tuple<String, IntegerMap<UnitType>>> getWhenCapturedChangesInto()
	{
		return m_whenCapturedChangesInto;
	}
	
	public void clearWhenCapturedChangesInto()
	{
		m_whenCapturedChangesInto.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setDestroyedWhenCapturedBy(String value) throws GameParseException
	{
		// We can prefix this value with "BY" or "FROM" to change the setting. If no setting, default to "BY" since this this is called by destroyedWhenCapturedBy
		String byOrFrom = "BY";
		if (value.startsWith("BY:") && getData().getPlayerList().getPlayerID("BY") == null)
		{
			byOrFrom = "BY";
			value = value.replaceFirst("BY:", "");
		}
		else if (value.startsWith("FROM:") && getData().getPlayerList().getPlayerID("FROM") == null)
		{
			byOrFrom = "FROM";
			value = value.replaceFirst("FROM:", "");
		}
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_destroyedWhenCapturedBy.add(new Tuple<String, PlayerID>(byOrFrom, tempPlayer));
			else
				throw new GameParseException("No player named: " + name + thisErrorMsg());
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDestroyedWhenCapturedBy(final ArrayList<Tuple<String, PlayerID>> value)
	{
		m_destroyedWhenCapturedBy = value;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setDestroyedWhenCapturedFrom(String value) throws GameParseException
	{
		if (!(value.startsWith("BY:") || value.startsWith("FROM:")))
		{
			value = "FROM:" + value;
		}
		setDestroyedWhenCapturedBy(value);
	}
	
	public ArrayList<Tuple<String, PlayerID>> getDestroyedWhenCapturedBy()
	{
		return m_destroyedWhenCapturedBy;
	}
	
	public void clearDestroyedWhenCapturedBy()
	{
		m_destroyedWhenCapturedBy.clear();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanBlitz(final String s)
	{
		m_canBlitz = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanBlitz(final Boolean s)
	{
		m_canBlitz = s;
	}
	
	public boolean getCanBlitz(final PlayerID player)
	{
		if (m_canBlitz)
			return true;
		if (TechAbilityAttachment.getUnitAbilitiesGained(TechAbilityAttachment.ABILITY_CAN_BLITZ, (UnitType) this.getAttachedTo(), player, getData()))
			return true;
		return false;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsSub(final String s)
	{
		m_isSub = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsSub(final Boolean s)
	{
		m_isSub = s;
	}
	
	public boolean getIsSub()
	{
		return m_isSub;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsCombatTransport(final String s)
	{
		m_isCombatTransport = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsCombatTransport(final Boolean s)
	{
		m_isCombatTransport = s;
	}
	
	public boolean getIsCombatTransport()
	{
		return m_isCombatTransport;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsStrategicBomber(final String s)
	{
		m_isStrategicBomber = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsStrategicBomber(final Boolean s)
	{
		m_isStrategicBomber = s;
	}
	
	public boolean getIsStrategicBomber()
	{
		return m_isStrategicBomber;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsDestroyer(final String s)
	{
		m_isDestroyer = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsDestroyer(final Boolean s)
	{
		m_isDestroyer = s;
	}
	
	public boolean getIsDestroyer()
	{
		return m_isDestroyer;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanBombard(final String s)
	{
		m_canBombard = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanBombard(final Boolean s)
	{
		m_canBombard = s;
	}
	
	public boolean getCanBombard(final PlayerID player)
	{
		if (m_canBombard)
			return true;
		if (TechAbilityAttachment.getUnitAbilitiesGained(TechAbilityAttachment.ABILITY_CAN_BOMBARD, (UnitType) this.getAttachedTo(), player, getData()))
			return true;
		return false;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAir(final String s)
	{
		m_isAir = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAir(final Boolean s)
	{
		m_isAir = s;
	}
	
	public boolean getIsAir()
	{
		return m_isAir;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsSea(final String s)
	{
		m_isSea = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsSea(final Boolean s)
	{
		m_isSea = s;
	}
	
	public boolean getIsSea()
	{
		return m_isSea;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsFactory(final String s)
	{
		m_isFactory = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsFactory(final Boolean s)
	{
		m_isFactory = s;
	}
	
	public boolean getIsFactory()
	{
		return m_isFactory;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanProduceUnits(final String s)
	{
		m_canProduceUnits = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanProduceUnits(final Boolean s)
	{
		m_canProduceUnits = s;
	}
	
	public boolean getCanProduceUnits()
	{
		return m_canProduceUnits;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanProduceXUnits(final String s)
	{
		m_canProduceXUnits = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanProduceXUnits(final Integer s)
	{
		m_canProduceXUnits = s;
	}
	
	public int getCanProduceXUnits()
	{
		return m_canProduceXUnits;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanOnlyBePlacedInTerritoryValuedAtX(final String s)
	{
		m_canOnlyBePlacedInTerritoryValuedAtX = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanOnlyBePlacedInTerritoryValuedAtX(final Integer s)
	{
		m_canOnlyBePlacedInTerritoryValuedAtX = s;
	}
	
	public int getCanOnlyBePlacedInTerritoryValuedAtX()
	{
		return m_canOnlyBePlacedInTerritoryValuedAtX;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setUnitPlacementRestrictions(final String value)
	{
		if (value == null)
		{
			m_unitPlacementRestrictions = null;
			return;
		}
		m_unitPlacementRestrictions = value.split(":");
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setUnitPlacementRestrictions(final String[] value)
	{
		m_unitPlacementRestrictions = value;
	}
	
	public String[] getUnitPlacementRestrictions()
	{
		return m_unitPlacementRestrictions;
	}
	
	// no m_ variable for this, since it is the inverse of m_unitPlacementRestrictions we might as well just use m_unitPlacementRestrictions
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setUnitPlacementOnlyAllowedIn(final String value) throws GameParseException
	{
		String valueRestricted = new String();
		final String valueAllowed[] = value.split(":");
		if (valueAllowed != null)
		{
			getListedTerritories(valueAllowed);
			final Collection<Territory> allTerrs = getData().getMap().getTerritories();
			for (final Territory item : allTerrs)
			{
				boolean match = false;
				for (final String allowed : valueAllowed)
				{
					if (allowed.matches(item.getName()))
						match = true;
				}
				if (!match)
					valueRestricted = valueRestricted + ":" + item.getName();
			}
			valueRestricted = valueRestricted.replaceFirst(":", "");
			m_unitPlacementRestrictions = valueRestricted.split(":");
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRepairsUnits(final String value)
	{
		if (value == null)
		{
			m_repairsUnits = null;
			return;
		}
		m_repairsUnits = value.split(":");
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRepairsUnits(final String[] value)
	{
		m_repairsUnits = value;
	}
	
	public String[] getRepairsUnits()
	{
		return m_repairsUnits;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setSpecial(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		for (final String option : s)
		{
			if (!(option.equals("none") || option.equals("canOnlyPlaceInOriginalTerritories")))
				throw new GameParseException("special does not allow: " + option + thisErrorMsg());
			m_special.add(option);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setSpecial(final HashSet<String> value)
	{
		m_special = value;
	}
	
	public HashSet<String> getSpecial()
	{
		return m_special;
	}
	
	public void clearSpecial()
	{
		m_special.clear();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanInvadeOnlyFrom(final String value)
	{
		if (value == null)
		{
			m_canInvadeOnlyFrom = null;
			return;
		}
		final String[] canOnlyInvadeFrom = value.split(":");
		if (canOnlyInvadeFrom[0].toLowerCase().equals("none"))
		{
			m_canInvadeOnlyFrom = new String[] { "none" };
			return;
		}
		if (canOnlyInvadeFrom[0].toLowerCase().equals("all"))
		{
			m_canInvadeOnlyFrom = new String[] { "all" };
			return;
		}
		m_canInvadeOnlyFrom = canOnlyInvadeFrom;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanInvadeOnlyFrom(final String[] value)
	{
		m_canInvadeOnlyFrom = value;
	}
	
	public boolean canInvadeFrom(final String transport)
	{
		final UnitType ut = getData().getUnitTypeList().getUnitType(transport);
		if (ut == null)
			throw new IllegalStateException("No unit called:" + transport + thisErrorMsg());
		// UnitAttachment ua = UnitAttachment.get(ut); //(UnitAttachment) ut.getAttachments().values().iterator().next();
		// Units may be considered transported if they are on a carrier, or if they are paratroopers, or if they are mech infantry. The "transporter" may not be an actual transport, so we should not check for that here.
		if (m_canInvadeOnlyFrom == null || Arrays.asList(m_canInvadeOnlyFrom).isEmpty() || m_canInvadeOnlyFrom[0].equals("") || m_canInvadeOnlyFrom[0].equals("all"))
		{
			return true;
		}
		return Arrays.asList(m_canInvadeOnlyFrom).contains(transport);
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setRequiresUnits(final String value)
	{
		m_requiresUnits.add(value.split(":"));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRequiresUnits(final ArrayList<String[]> value)
	{
		m_requiresUnits = value;
	}
	
	public ArrayList<String[]> getRequiresUnits()
	{
		return m_requiresUnits;
	}
	
	public void clearRequiresUnits()
	{
		m_requiresUnits.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setWhenCombatDamaged(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (!(s.length == 3 || s.length == 4))
			throw new GameParseException("whenCombatDamaged must have 3 or 4 parts: value=effect:optionalNumber, count=integer:integer" + thisErrorMsg());
		final int from = getInt(s[0]);
		final int to = getInt(s[1]);
		if (from < 0 || to < 0 || to < from)
			throw new GameParseException("whenCombatDamaged damaged integers must be positive, and the second integer must be equal to or greater than the first" + thisErrorMsg());
		final Tuple<Integer, Integer> fromTo = new Tuple<Integer, Integer>(from, to);
		Tuple<String, String> effectNum;
		if (s.length == 3)
			effectNum = new Tuple<String, String>(s[2], null);
		else
			effectNum = new Tuple<String, String>(s[2], s[3]);
		m_whenCombatDamaged.add(new Tuple<Tuple<Integer, Integer>, Tuple<String, String>>(fromTo, effectNum));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWhenCombatDamaged(final ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> value)
	{
		m_whenCombatDamaged = value;
	}
	
	public ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> getWhenCombatDamaged()
	{
		return m_whenCombatDamaged;
	}
	
	public void clearWhenCombatDamaged()
	{
		m_whenCombatDamaged.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setReceivesAbilityWhenWith(final String value)
	{
		m_receivesAbilityWhenWith.add(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setReceivesAbilityWhenWith(final ArrayList<String> value)
	{
		m_receivesAbilityWhenWith = value;
	}
	
	public ArrayList<String> getReceivesAbilityWhenWith()
	{
		return m_receivesAbilityWhenWith;
	}
	
	public void clearReceivesAbilityWhenWith()
	{
		m_receivesAbilityWhenWith.clear();
	}
	
	public static IntegerMap<Tuple<String, String>> getReceivesAbilityWhenWithMap(final Collection<Unit> units, final String filterForAbility, final GameData data)
	{
		final IntegerMap<Tuple<String, String>> map = new IntegerMap<Tuple<String, String>>();
		final Collection<UnitType> canReceive = getUnitTypesFromUnitList(Match.getMatches(units, Matches.UnitCanReceivesAbilityWhenWith()));
		for (final UnitType ut : canReceive)
		{
			final Collection<String> receives = UnitAttachment.get(ut).getReceivesAbilityWhenWith();
			for (final String receive : receives)
			{
				final String[] s = receive.split(":");
				if (filterForAbility != null && !filterForAbility.equals(s[0]))
					continue;
				map.put(new Tuple<String, String>(s[0], s[1]), Match.countMatches(units, Matches.unitIsOfType(data.getUnitTypeList().getUnitType(s[1]))));
			}
		}
		return map;
	}
	
	public static Collection<Unit> getUnitsWhichReceivesAbilityWhenWith(final Collection<Unit> units, final String filterForAbility, final GameData data)
	{
		if (Match.noneMatch(units, Matches.UnitCanReceivesAbilityWhenWith()))
			return new ArrayList<Unit>();
		final Collection<Unit> unitsCopy = new ArrayList<Unit>(units);
		final HashSet<Unit> whichReceiveNoDuplicates = new HashSet<Unit>();
		final IntegerMap<Tuple<String, String>> whichGive = getReceivesAbilityWhenWithMap(unitsCopy, filterForAbility, data);
		for (final Tuple<String, String> abilityUnitType : whichGive.keySet())
		{
			final Collection<Unit> receives = Match.getNMatches(unitsCopy, whichGive.getInt(abilityUnitType), Matches.UnitCanReceivesAbilityWhenWith(filterForAbility, abilityUnitType.getSecond()));
			whichReceiveNoDuplicates.addAll(receives);
			unitsCopy.removeAll(receives);
		}
		return whichReceiveNoDuplicates;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsConstruction(final String s)
	{
		m_isConstruction = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsConstruction(final Boolean s)
	{
		m_isConstruction = s;
	}
	
	public boolean getIsConstruction()
	{
		return m_isConstruction;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setConstructionType(final String s)
	{
		m_constructionType = s;
	}
	
	public String getConstructionType()
	{
		return m_constructionType;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setConstructionsPerTerrPerTypePerTurn(final String s)
	{
		m_constructionsPerTerrPerTypePerTurn = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setConstructionsPerTerrPerTypePerTurn(final Integer s)
	{
		m_constructionsPerTerrPerTypePerTurn = s;
	}
	
	public int getConstructionsPerTerrPerTypePerTurn()
	{
		return m_constructionsPerTerrPerTypePerTurn;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxConstructionsPerTypePerTerr(final String s)
	{
		m_maxConstructionsPerTypePerTerr = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxConstructionsPerTypePerTerr(final Integer s)
	{
		m_maxConstructionsPerTypePerTerr = s;
	}
	
	public int getMaxConstructionsPerTypePerTerr()
	{
		return m_maxConstructionsPerTypePerTerr;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsMarine(final String s)
	{
		m_isMarine = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsMarine(final Boolean s)
	{
		m_isMarine = s;
	}
	
	public boolean getIsMarine()
	{
		return m_isMarine;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsInfantry(final String s)
	{
		m_isInfantry = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsInfantry(final Boolean s)
	{
		m_isInfantry = s;
	}
	
	public boolean getIsInfantry()
	{
		return m_isInfantry;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsLandTransport(final String s)
	{
		m_isLandTransport = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsLandTransport(final Boolean s)
	{
		m_isLandTransport = s;
	}
	
	public boolean isLandTransport()
	{
		return m_isLandTransport;
	}
	
	public boolean getIsLandTransport()
	{
		return m_isLandTransport;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTransportCapacity(final String s)
	{
		m_transportCapacity = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTransportCapacity(final Integer s)
	{
		m_transportCapacity = s;
	}
	
	public int getTransportCapacity()
	{
		return m_transportCapacity;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsTwoHit(final String s)
	{
		m_isTwoHit = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsTwoHit(final Boolean s)
	{
		m_isTwoHit = s;
	}
	
	public boolean getIsTwoHit()
	{
		return m_isTwoHit;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTransportCost(final String s)
	{
		m_transportCost = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTransportCost(final Integer s)
	{
		m_transportCost = s;
	}
	
	public int getTransportCost()
	{
		return m_transportCost;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxBuiltPerPlayer(final String s)
	{
		m_maxBuiltPerPlayer = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxBuiltPerPlayer(final Integer s)
	{
		m_maxBuiltPerPlayer = s;
	}
	
	public int getMaxBuiltPerPlayer()
	{
		return m_maxBuiltPerPlayer;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCarrierCapacity(final String s)
	{
		m_carrierCapacity = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCarrierCapacity(final Integer s)
	{
		m_carrierCapacity = s;
	}
	
	public int getCarrierCapacity()
	{
		return m_carrierCapacity;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCarrierCost(final String s)
	{
		m_carrierCost = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCarrierCost(final Integer s)
	{
		m_carrierCost = s;
	}
	
	public int getCarrierCost()
	{
		return m_carrierCost;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = false, adds = false)
	public void setArtillery(final String s) throws GameParseException
	{
		m_artillery = getBool(s);
		if (m_artillery)
			UnitSupportAttachment.addRule((UnitType) getAttachedTo(), getData(), false);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = false, adds = false)
	public void setArtillery(final Boolean s) throws GameParseException
	{
		m_artillery = s;
		if (m_artillery)
			UnitSupportAttachment.addRule((UnitType) getAttachedTo(), getData(), false);
	}
	
	public boolean getArtillery()
	{
		return m_artillery;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = false, adds = false)
	public void setArtillerySupportable(final String s) throws GameParseException
	{
		m_artillerySupportable = getBool(s);
		if (m_artillerySupportable)
			UnitSupportAttachment.addTarget((UnitType) getAttachedTo(), getData());
	}
	
	@GameProperty(xmlProperty = true, gameProperty = false, adds = false)
	public void setArtillerySupportable(final Boolean s) throws GameParseException
	{
		m_artillerySupportable = s;
		if (m_artillerySupportable)
			UnitSupportAttachment.addTarget((UnitType) getAttachedTo(), getData());
	}
	
	public boolean getArtillerySupportable()
	{
		return m_artillerySupportable;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = false, adds = false)
	public void setUnitSupportCount(final String s)
	{
		m_unitSupportCount = getInt(s);
		UnitSupportAttachment.setOldSupportCount((UnitType) getAttachedTo(), getData(), s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = false, adds = false)
	public void setUnitSupportCount(final Integer s)
	{
		m_unitSupportCount = s;
		UnitSupportAttachment.setOldSupportCount((UnitType) getAttachedTo(), getData(), s.toString());
	}
	
	public int getUnitSupportCount()
	{
		return m_unitSupportCount > 0 ? m_unitSupportCount : 1;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBombard(final String s)
	{
		m_bombard = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBombard(final Integer s)
	{
		m_bombard = s;
	}
	
	public int getBombard(final PlayerID player)
	{
		return m_bombard > 0 ? m_bombard : m_attack;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMovement(final String s)
	{
		m_movement = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMovement(final Integer s)
	{
		m_movement = s;
	}
	
	public int getMovement(final PlayerID player)
	{
		return Math.max(0, m_movement + TechAbilityAttachment.getMovementBonus((UnitType) this.getAttachedTo(), player, getData()));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttack(final String s)
	{
		m_attack = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttack(final Integer s)
	{
		m_attack = s;
	}
	
	public int getAttack(final PlayerID player)
	{
		int attackValue = m_attack + TechAbilityAttachment.getAttackBonus((UnitType) this.getAttachedTo(), player, getData());
		if (attackValue > 0 && player.isAI())
		{
			attackValue += games.strategy.triplea.Properties.getAIBonusAttack(getData());
		}
		return Math.min(getData().getDiceSides(), Math.max(0, attackValue));
	}
	
	int getRawAttack()
	{
		return m_attack;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackRolls(final String s)
	{
		m_attackRolls = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackRolls(final Integer s)
	{
		m_attackRolls = s;
	}
	
	public int getAttackRolls(final PlayerID player)
	{
		if (getAttack(player) <= 0)
			return 0;
		return Math.max(0, m_attackRolls + TechAbilityAttachment.getAttackRollsBonus((UnitType) this.getAttachedTo(), player, getData()));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDefense(final String s)
	{
		m_defense = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDefense(final Integer s)
	{
		m_defense = s;
	}
	
	public int getDefense(final PlayerID player)
	{
		int defenseValue = m_defense + TechAbilityAttachment.getDefenseBonus((UnitType) this.getAttachedTo(), player, getData());
		if (defenseValue > 0 && m_isSub && TechTracker.hasSuperSubs(player))
		{
			final int bonus = games.strategy.triplea.Properties.getSuper_Sub_Defense_Bonus(getData());
			defenseValue += bonus;
		}
		if (defenseValue > 0 && player.isAI())
		{
			defenseValue += games.strategy.triplea.Properties.getAIBonusDefense(getData());
		}
		return Math.min(getData().getDiceSides(), Math.max(0, defenseValue));
	}
	
	int getRawDefense()
	{
		return m_defense;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDefenseRolls(final String s)
	{
		m_defenseRolls = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDefenseRolls(final Integer s)
	{
		m_defenseRolls = s;
	}
	
	public int getDefenseRolls(final PlayerID player)
	{
		if (getDefense(player) <= 0)
			return 0;
		return Math.max(0, m_defenseRolls + TechAbilityAttachment.getDefenseRollsBonus((UnitType) this.getAttachedTo(), player, getData()));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setChooseBestRoll(final String s)
	{
		m_chooseBestRoll = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setChooseBestRoll(final Boolean s)
	{
		m_chooseBestRoll = s;
	}
	
	public boolean getChooseBestRoll()
	{
		return m_chooseBestRoll;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanScramble(final String s)
	{
		m_canScramble = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanScramble(final Boolean s)
	{
		m_canScramble = s;
	}
	
	public boolean getCanScramble()
	{
		return m_canScramble;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxScrambleCount(final String s)
	{
		m_maxScrambleCount = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxScrambleCount(final Integer s)
	{
		m_maxScrambleCount = s;
	}
	
	public int getMaxScrambleCount()
	{
		return m_maxScrambleCount;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxScrambleDistance(final String s)
	{
		m_maxScrambleDistance = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxScrambleDistance(final Integer s)
	{
		m_maxScrambleDistance = s;
	}
	
	public int getMaxScrambleDistance()
	{
		return m_maxScrambleDistance;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxOperationalDamage(final String s)
	{
		m_maxOperationalDamage = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxOperationalDamage(final Integer s)
	{
		m_maxOperationalDamage = s;
	}
	
	public int getMaxOperationalDamage()
	{
		return m_maxOperationalDamage;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxDamage(final String s)
	{
		m_maxDamage = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxDamage(final Integer s)
	{
		m_maxDamage = s;
	}
	
	public int getMaxDamage()
	{
		return m_maxDamage;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAirBase(final String s)
	{
		m_isAirBase = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAirBase(final Boolean s)
	{
		m_isAirBase = s;
	}
	
	public boolean getIsAirBase()
	{
		return m_isAirBase;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsInfrastructure(final String s)
	{
		m_isInfrastructure = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsInfrastructure(final Boolean s)
	{
		m_isInfrastructure = s;
	}
	
	public boolean getIsInfrastructure()
	{
		return m_isInfrastructure;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanBeDamaged(final String s)
	{
		m_canBeDamaged = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanBeDamaged(final Boolean s)
	{
		m_canBeDamaged = s;
	}
	
	public boolean getCanBeDamaged()
	{
		return m_canBeDamaged;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanDieFromReachingMaxDamage(final String s)
	{
		m_canDieFromReachingMaxDamage = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanDieFromReachingMaxDamage(final Boolean s)
	{
		m_canDieFromReachingMaxDamage = s;
	}
	
	public boolean getCanDieFromReachingMaxDamage()
	{
		return m_canDieFromReachingMaxDamage;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsSuicide(final String s)
	{
		m_isSuicide = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsSuicide(final Boolean s)
	{
		m_isSuicide = s;
	}
	
	public boolean getIsSuicide()
	{
		return m_isSuicide;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsKamikaze(final String s)
	{
		m_isKamikaze = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsKamikaze(final Boolean s)
	{
		m_isKamikaze = s;
	}
	
	public boolean getIsKamikaze()
	{
		return m_isKamikaze;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBlockade(final String s)
	{
		m_blockade = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBlockade(final Integer s)
	{
		m_blockade = s;
	}
	
	public int getBlockade()
	{
		return m_blockade;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setGivesMovement(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("givesMovement can not be empty or have more than two fields" + thisErrorMsg());
		String unitTypeToProduce;
		unitTypeToProduce = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitTypeToProduce + thisErrorMsg());
		// we should allow positive and negative numbers, since you can give bonuses to units or take away a unit's movement
		final int n = getInt(s[0]);
		m_givesMovement.put(ut, n);
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
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setConsumesUnits(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("consumesUnits can not be empty or have more than two fields" + thisErrorMsg());
		String unitTypeToProduce;
		unitTypeToProduce = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitTypeToProduce + thisErrorMsg());
		final int n = getInt(s[0]);
		if (n < 1)
			throw new GameParseException("consumesUnits must have positive values" + thisErrorMsg());
		m_consumesUnits.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setConsumesUnits(final IntegerMap<UnitType> value)
	{
		m_consumesUnits = value;
	}
	
	public IntegerMap<UnitType> getConsumesUnits()
	{
		return m_consumesUnits;
	}
	
	public void clearConsumesUnits()
	{
		m_consumesUnits.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setCreatesUnitsList(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("createsUnitsList can not be empty or have more than two fields" + thisErrorMsg());
		String unitTypeToProduce;
		unitTypeToProduce = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
		if (ut == null)
			throw new GameParseException("createsUnitsList: No unit called:" + unitTypeToProduce + thisErrorMsg());
		final int n = getInt(s[0]);
		if (n < 1)
			throw new GameParseException("createsUnitsList must have positive values" + thisErrorMsg());
		m_createsUnitsList.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCreatesUnitsList(final IntegerMap<UnitType> value)
	{
		m_createsUnitsList = value;
	}
	
	public IntegerMap<UnitType> getCreatesUnitsList()
	{
		return m_createsUnitsList;
	}
	
	public void clearCreatesUnitsList()
	{
		m_createsUnitsList.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setCreatesResourcesList(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("createsResourcesList can not be empty or have more than two fields" + thisErrorMsg());
		String resourceToProduce;
		resourceToProduce = s[1];
		// validate that this resource exists in the xml
		final Resource r = getData().getResourceList().getResource(resourceToProduce);
		if (r == null)
			throw new GameParseException("createsResourcesList: No resource called:" + resourceToProduce + thisErrorMsg());
		final int n = getInt(s[0]);
		m_createsResourcesList.put(r, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCreatesResourcesList(final IntegerMap<Resource> value)
	{
		m_createsResourcesList = value;
	}
	
	public IntegerMap<Resource> getCreatesResourcesList()
	{
		return m_createsResourcesList;
	}
	
	public void clearCreatesResourcesList()
	{
		m_createsResourcesList.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setFuelCost(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("fuelCost must have two fields" + thisErrorMsg());
		String resourceToProduce;
		resourceToProduce = s[1];
		// validate that this resource exists in the xml
		final Resource r = getData().getResourceList().getResource(resourceToProduce);
		if (r == null)
			throw new GameParseException("fuelCost: No resource called:" + resourceToProduce + thisErrorMsg());
		final int n = getInt(s[0]);
		if (n < 0)
			throw new GameParseException("fuelCost must have positive values" + thisErrorMsg());
		m_fuelCost.put(r, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setFuelCost(final IntegerMap<Resource> value)
	{
		m_fuelCost = value;
	}
	
	public IntegerMap<Resource> getFuelCost()
	{
		return m_fuelCost;
	}
	
	public void clearFuelCost()
	{
		m_fuelCost.clear();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBombingBonus(final String s)
	{
		m_bombingBonus = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBombingBonus(final Integer s)
	{
		m_bombingBonus = s;
	}
	
	public int getBombingBonus()
	{
		return m_bombingBonus;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBombingMaxDieSides(final String s)
	{
		m_bombingMaxDieSides = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBombingMaxDieSides(final Integer s)
	{
		m_bombingMaxDieSides = s;
	}
	
	public int getBombingMaxDieSides()
	{
		return m_bombingMaxDieSides;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setBombingTargets(final String value) throws GameParseException
	{
		if (value == null)
		{
			m_bombingTargets = null;
			return;
		}
		if (m_bombingTargets == null)
			m_bombingTargets = new HashSet<UnitType>();
		final String[] s = value.split(":");
		for (final String u : s)
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(u);
			if (ut == null)
				throw new GameParseException("bombingTargets: no such unit type: " + u + thisErrorMsg());
			m_bombingTargets.add(ut);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBombingTargets(final HashSet<UnitType> value)
	{
		m_bombingTargets = value;
	}
	
	public HashSet<UnitType> getBombingTargets(final GameData data)
	{
		if (m_bombingTargets != null)
			return m_bombingTargets;
		return new HashSet<UnitType>(data.getUnitTypeList().getAllUnitTypes());
	}
	
	public void clearBombingTargets()
	{
		m_bombingTargets.clear();
	}
	
	public static Set<UnitType> getAllowedBombingTargetsIntersection(final Collection<Unit> bombersOrRockets, final GameData data)
	{
		if (bombersOrRockets.isEmpty())
			return new HashSet<UnitType>();
		Collection<UnitType> allowedTargets = data.getUnitTypeList().getAllUnitTypes();
		for (final Unit u : bombersOrRockets)
		{
			final UnitAttachment ua = UnitAttachment.get(u.getType());
			final HashSet<UnitType> bombingTargets = ua.getBombingTargets(data);
			if (bombingTargets != null)
				allowedTargets = games.strategy.util.Util.intersection(allowedTargets, bombingTargets);
		}
		return new HashSet<UnitType>(allowedTargets);
	}
	
	// Do not delete, we keep this both for backwards compatibility, and for user convenience when making maps
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAA(final String s) throws GameParseException
	{
		getBool(s);
		setIsAAforCombatOnly(s);
		setIsAAforBombingThisUnitOnly(s);
		setIsAAforFlyOverOnly(s);
		setIsAAmovement(s);
		setIsRocket(s);
		setIsInfrastructure(s);
	}
	
	// Do not delete, we keep this both for backwards compatibility, and for user convenience when making maps
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAA(final Boolean s) throws GameParseException
	{
		setIsAAforCombatOnly(s);
		setIsAAforBombingThisUnitOnly(s);
		setIsAAforFlyOverOnly(s);
		setIsAAmovement(s);
		setIsRocket(s);
		setIsInfrastructure(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackAA(final String s)
	{
		m_attackAA = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackAA(final Integer s)
	{
		m_attackAA = s;
	}
	
	public int getAttackAA(final PlayerID player)
	{
		// TODO: this may cause major problems with Low Luck, if they have diceSides equal to something other than 6, or it does not divide perfectly into attackAAmaxDieSides
		return Math.max(0, Math.min(getAttackAAmaxDieSides(), m_attackAA + TechAbilityAttachment.getRadarBonus((UnitType) this.getAttachedTo(), player, getData())));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackAAmaxDieSides(final String s)
	{
		m_attackAAmaxDieSides = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackAAmaxDieSides(final Integer s)
	{
		m_attackAAmaxDieSides = s;
	}
	
	public int getAttackAAmaxDieSides()
	{
		if (m_attackAAmaxDieSides < 0)
			return getData().getDiceSides();
		return m_attackAAmaxDieSides;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxAAattacks(final String s) throws GameParseException
	{
		final int attacks = getInt(s);
		if (attacks < -1)
			throw new GameParseException("maxAAattacks must be positive" + thisErrorMsg());
		m_maxAAattacks = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMaxAAattacks(final Integer s)
	{
		m_maxAAattacks = s;
	}
	
	public int getMaxAAattacks()
	{
		return m_maxAAattacks;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMayOverStackAA(final String s)
	{
		m_mayOverStackAA = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMayOverStackAA(final Boolean s)
	{
		m_mayOverStackAA = s;
	}
	
	public boolean getMayOverStackAA()
	{
		return m_mayOverStackAA;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAAforCombatOnly(final String s)
	{
		m_isAAforCombatOnly = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAAforCombatOnly(final Boolean s)
	{
		m_isAAforCombatOnly = s;
	}
	
	public boolean getIsAAforCombatOnly()
	{
		return m_isAAforCombatOnly;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAAforBombingThisUnitOnly(final String s)
	{
		m_isAAforBombingThisUnitOnly = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAAforBombingThisUnitOnly(final Boolean s)
	{
		m_isAAforBombingThisUnitOnly = s;
	}
	
	public boolean getIsAAforBombingThisUnitOnly()
	{
		return m_isAAforBombingThisUnitOnly;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAAforFlyOverOnly(final String s)
	{
		m_isAAforFlyOverOnly = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAAforFlyOverOnly(final Boolean s)
	{
		m_isAAforFlyOverOnly = s;
	}
	
	public boolean getIsAAforFlyOverOnly()
	{
		return m_isAAforFlyOverOnly;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsRocket(final String s)
	{
		m_isRocket = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsRocket(final Boolean s)
	{
		m_isRocket = s;
	}
	
	public boolean getIsRocket()
	{
		return m_isRocket;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTypeAA(final String s)
	{
		m_typeAA = s;
	}
	
	public String getTypeAA()
	{
		return m_typeAA;
	}
	
	public static Set<String> getAllOfTypeAAs(final Collection<Unit> aaUnits, final Collection<Unit> targets, final Match<Unit> typeOfAA,
				final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed)
	{
		final Set<String> rVal = new HashSet<String>();
		for (final Unit u : Match.getMatches(aaUnits, Matches.UnitIsAAthatCanHitTheseUnits(targets, typeOfAA, airborneTechTargetsAllowed)))
		{
			rVal.add(UnitAttachment.get(u.getType()).getTypeAA());
		}
		return rVal;
	}
	
	public static Set<String> getAllOfTypeAAs(final Collection<Unit> aaUnitsAlreadyVerified)
	{
		final Set<String> rVal = new HashSet<String>();
		for (final Unit u : aaUnitsAlreadyVerified)
		{
			rVal.add(UnitAttachment.get(u.getType()).getTypeAA());
		}
		return rVal;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setTargetsAA(final String value) throws GameParseException
	{
		if (value == null)
		{
			m_targetsAA = null;
			return;
		}
		if (m_targetsAA == null)
			m_targetsAA = new HashSet<UnitType>();
		final String[] s = value.split(":");
		for (final String u : s)
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(u);
			if (ut == null)
				throw new GameParseException("AAtargets: no such unit type: " + u + thisErrorMsg());
			m_targetsAA.add(ut);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTargetsAA(final HashSet<UnitType> value)
	{
		m_targetsAA = value;
	}
	
	public HashSet<UnitType> getTargetsAA(final GameData data)
	{
		if (m_targetsAA != null)
			return m_targetsAA;
		final HashSet<UnitType> airTypes = new HashSet<UnitType>();
		final Iterator<UnitType> utIter = data.getUnitTypeList().iterator();
		while (utIter.hasNext())
		{
			final UnitType ut = utIter.next();
			if (UnitAttachment.get(ut).getIsAir())
				airTypes.add(ut);
		}
		return airTypes;
	}
	
	public void clearTargetsAA()
	{
		m_targetsAA.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setWillNotFireIfPresent(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		for (final String u : s)
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(u);
			if (ut == null)
				throw new GameParseException("willNotFireIfPresent: no such unit type: " + u + thisErrorMsg());
			m_willNotFireIfPresent.add(ut);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWillNotFireIfPresent(final HashSet<UnitType> value)
	{
		m_willNotFireIfPresent = value;
	}
	
	public HashSet<UnitType> getWillNotFireIfPresent()
	{
		return m_willNotFireIfPresent;
	}
	
	public void clearWillNotFireIfPresent()
	{
		m_willNotFireIfPresent.clear();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAAmovement(final String s) throws GameParseException
	{
		setCanNotMoveDuringCombatMove(s);
		if (getBool(s))
		{
			setMovementLimit(Integer.MAX_VALUE + ":allied");
			setAttackingLimit(Integer.MAX_VALUE + ":allied");
			setPlacementLimit(Integer.MAX_VALUE + ":allied");
		}
		else
		{
			m_movementLimit = null;
			m_attackingLimit = null;
			m_placementLimit = null;
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsAAmovement(final Boolean s) throws GameParseException
	{
		setCanNotMoveDuringCombatMove(s);
		if (s)
		{
			setMovementLimit(Integer.MAX_VALUE + ":allied");
			setAttackingLimit(Integer.MAX_VALUE + ":allied");
			setPlacementLimit(Integer.MAX_VALUE + ":allied");
		}
		else
		{
			m_movementLimit = null;
			m_attackingLimit = null;
			m_placementLimit = null;
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanNotMoveDuringCombatMove(final String s)
	{
		m_canNotMoveDuringCombatMove = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanNotMoveDuringCombatMove(final Boolean s)
	{
		m_canNotMoveDuringCombatMove = s;
	}
	
	public boolean getCanNotMoveDuringCombatMove()
	{
		return m_canNotMoveDuringCombatMove;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMovementLimit(final String value) throws GameParseException
	{
		if (value == null)
		{
			m_movementLimit = null;
			return;
		}
		final UnitType ut = (UnitType) this.getAttachedTo();
		if (ut == null)
			throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
		final String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("movementLimit must have 2 fields, value and count" + thisErrorMsg());
		final int max = getInt(s[0]);
		if (max < 0)
			throw new GameParseException("movementLimit count must have a positive number" + thisErrorMsg());
		if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total")))
			throw new GameParseException("movementLimit value must owned, allied, or total" + thisErrorMsg());
		m_movementLimit = new Tuple<Integer, String>(max, s[1]);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMovementLimit(final Tuple<Integer, String> value)
	{
		m_movementLimit = value;
	}
	
	public Tuple<Integer, String> getMovementLimit()
	{
		return m_movementLimit;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackingLimit(final String value) throws GameParseException
	{
		if (value == null)
		{
			m_attackingLimit = null;
			return;
		}
		final UnitType ut = (UnitType) this.getAttachedTo();
		if (ut == null)
			throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
		final String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("attackingLimit must have 2 fields, value and count" + thisErrorMsg());
		final int max = getInt(s[0]);
		if (max < 0)
			throw new GameParseException("attackingLimit count must have a positive number" + thisErrorMsg());
		if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total")))
			throw new GameParseException("attackingLimit value must owned, allied, or total" + thisErrorMsg());
		m_attackingLimit = new Tuple<Integer, String>(max, s[1]);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackingLimit(final Tuple<Integer, String> value)
	{
		m_attackingLimit = value;
	}
	
	public Tuple<Integer, String> getAttackingLimit()
	{
		return m_attackingLimit;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setPlacementLimit(final String value) throws GameParseException
	{
		if (value == null)
		{
			m_placementLimit = null;
			return;
		}
		final UnitType ut = (UnitType) this.getAttachedTo();
		if (ut == null)
			throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
		final String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("placementLimit must have 2 fields, value and count" + thisErrorMsg());
		final int max = getInt(s[0]);
		if (max < 0)
			throw new GameParseException("placementLimit count must have a positive number" + thisErrorMsg());
		if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total")))
			throw new GameParseException("placementLimit value must owned, allied, or total" + thisErrorMsg());
		m_placementLimit = new Tuple<Integer, String>(max, s[1]);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setPlacementLimit(final Tuple<Integer, String> value)
	{
		m_placementLimit = value;
	}
	
	public Tuple<Integer, String> getPlacementLimit()
	{
		return m_placementLimit;
	}
	
	public static int getMaximumNumberOfThisUnitTypeToReachStackingLimit(final String limitType, final UnitType ut, final Territory t, final PlayerID owner, final GameData data)
	{
		final UnitAttachment ua = UnitAttachment.get(ut);
		final Tuple<Integer, String> stackingLimit;
		if (limitType.equals("movementLimit"))
			stackingLimit = ua.getMovementLimit();
		else if (limitType.equals("attackingLimit"))
			stackingLimit = ua.getAttackingLimit();
		else if (limitType.equals("placementLimit"))
			stackingLimit = ua.getPlacementLimit();
		else
			throw new IllegalStateException("getMaximumNumberOfThisUnitTypeToReachStackingLimit does not allow limitType: " + limitType);
		if (stackingLimit == null)
			return Integer.MAX_VALUE;
		int max = stackingLimit.getFirst();
		if (max == Integer.MAX_VALUE && (ua.getIsAAforBombingThisUnitOnly() || ua.getIsAAforCombatOnly()))
		{
			// under certain rules (classic rules) there can only be 1 aa gun in a territory.
			if (!(games.strategy.triplea.Properties.getWW2V2(data) || games.strategy.triplea.Properties.getWW2V3(data) || games.strategy.triplea.Properties.getMultipleAAPerTerritory(data)))
			{
				max = 1;
			}
		}
		final CompositeMatchAnd<Unit> stackingMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOfType(ut));
		final String stackingType = stackingLimit.getSecond();
		if (stackingType.equals("owned"))
			stackingMatch.add(Matches.unitIsOwnedBy(owner));
		else if (stackingType.equals("allied"))
			stackingMatch.add(Matches.isUnitAllied(owner, data));
		// else if (stackingType.equals("total"))
		final int totalInTerritory = Match.countMatches(t.getUnits().getUnits(), stackingMatch);
		return Math.max(0, max - totalInTerritory);
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		if (m_isAir)
		{
			if (m_isSea || m_isFactory || m_isSub || m_transportCost != -1 ||
						m_carrierCapacity != -1 || m_canBlitz || m_canBombard || m_isMarine || m_isInfantry || m_isLandTransport || m_isAirTransportable || m_isCombatTransport)
				throw new GameParseException("air units can not have certain properties, " + thisErrorMsg());
		}
		else if (m_isSea)
		{
			if (m_canIntercept || m_canEscort || m_canBlitz || m_isAir || m_isFactory || m_isStrategicBomber || m_carrierCost != -1
						|| m_transportCost != -1 || m_isMarine || m_isInfantry || m_isLandTransport || m_isAirTransportable || m_isAirTransport || m_isKamikaze)
				throw new GameParseException("sea units can not have certain properties, " + thisErrorMsg());
		}
		else
		// if land
		{
			if (m_canIntercept || m_canEscort || m_canBombard || m_isStrategicBomber || m_isSub || m_carrierCapacity != -1 || m_bombard != -1 || m_transportCapacity != -1 || m_isAirTransport
						|| m_isCombatTransport || m_isKamikaze)
				throw new GameParseException("land units can not have certain properties, " + thisErrorMsg());
		}
		if (m_attackAA < 0 || m_attackAAmaxDieSides < -1 || m_attackAAmaxDieSides > 200)
		{
			throw new GameParseException("attackAA or attackAAmaxDieSides is wrong, " + thisErrorMsg());
		}
		if (m_carrierCapacity != -1 && m_carrierCost != -1)
		{
			throw new GameParseException("carrierCost and carrierCapacity can not be set at same time, " + thisErrorMsg());
		}
		if (m_transportCost != -1 && m_transportCapacity != -1)
		{
			throw new GameParseException("transportCost and transportCapacity can not be set at same time, " + thisErrorMsg());
		}
		if (((m_bombingBonus >= 0 || m_bombingMaxDieSides >= 0) && !(m_isStrategicBomber || m_isRocket)) || (m_bombingBonus < -1 || m_bombingMaxDieSides < -1)
					|| (m_bombingBonus > 10000 || m_bombingMaxDieSides > 200))
		{
			throw new GameParseException("something wrong with bombingBonus or bombingMaxDieSides, " + thisErrorMsg());
		}
		if (m_maxBuiltPerPlayer < -1)
		{
			throw new GameParseException("maxBuiltPerPlayer can not be negative, " + thisErrorMsg());
		}
		if (m_isCombatTransport && m_transportCapacity < 1)
		{
			throw new GameParseException("can not have isCombatTransport on unit without transportCapacity, " + thisErrorMsg());
		}
		if (m_isSea && m_transportCapacity != -1 && Properties.getTransportCasualtiesRestricted(data) && (m_attack > 0 || m_defense > 0) && !m_isCombatTransport)
		{
			throw new GameParseException("Restricted transports cannot have attack or defense, " + thisErrorMsg());
		}
		if (m_isConstruction
					&& (m_constructionType == null || m_constructionType.equals("none") || m_constructionType.equals("") || m_constructionsPerTerrPerTypePerTurn < 0 || m_maxConstructionsPerTypePerTerr < 0))
		{
			throw new GameParseException("Constructions must have constructionType and positive constructionsPerTerrPerType and maxConstructionsPerType, " + thisErrorMsg());
		}
		if (!m_isConstruction
					&& (!(m_constructionType == null || m_constructionType.equals("none") || m_constructionType.equals("")) || m_constructionsPerTerrPerTypePerTurn >= 0 || m_maxConstructionsPerTypePerTerr >= 0))
		{
			throw new GameParseException("Constructions must have isConstruction true, " + thisErrorMsg());
		}
		if (m_constructionsPerTerrPerTypePerTurn > m_maxConstructionsPerTypePerTerr)
		{
			throw new GameParseException("Constructions must have constructionsPerTerrPerTypePerTurn Less than maxConstructionsPerTypePerTerr, " + thisErrorMsg());
		}
		if (m_unitPlacementRestrictions != null)
			getListedTerritories(m_unitPlacementRestrictions);
		if (m_repairsUnits != null)
			getListedUnits(m_repairsUnits);
		if (m_requiresUnits != null)
		{
			for (final String[] combo : m_requiresUnits)
				getListedUnits(combo);
		}
		if ((m_canBeDamaged && (m_maxDamage < 1)) || (!m_canBeDamaged && !m_isFactory && (m_maxDamage >= 0)) || (m_canDieFromReachingMaxDamage && !(m_maxDamage >= 0 || m_isFactory))
					|| (m_canBeDamaged && m_isFactory))
		{
			throw new GameParseException("something wrong with canBeDamaged or maxDamage or canDieFromReachingMaxDamage or isFactory, " + thisErrorMsg());
		}
		if (m_canInvadeOnlyFrom != null && !m_canInvadeOnlyFrom[0].equals("all") && !m_canInvadeOnlyFrom[0].equals("none"))
		{
			for (final String transport : m_canInvadeOnlyFrom)
			{
				final UnitType ut = getData().getUnitTypeList().getUnitType(transport);
				if (ut == null)
					throw new GameParseException("No unit called:" + transport + thisErrorMsg());
				if (ut.getAttachments() == null || ut.getAttachments().isEmpty())
					throw new GameParseException(transport + " has no attachments, please declare " + transport + " in the xml before using it as a transport" + thisErrorMsg());
				// Units may be considered transported if they are on a carrier, or if they are paratroopers, or if they are mech infantry. The "transporter" may not be an actual transport, so we should not check for that here.
			}
		}
		if (!m_receivesAbilityWhenWith.isEmpty())
		{
			for (final String value : m_receivesAbilityWhenWith)
			{
				// first is ability, second is unit that we get it from
				final String[] s = value.split(":");
				if (s.length != 2)
					throw new GameParseException("receivesAbilityWhenWith must have 2 parts, 'ability:unit'" + thisErrorMsg());
				if (getData().getUnitTypeList().getUnitType(s[1]) == null)
					throw new GameParseException("receivesAbilityWhenWith, unit does not exist, name:" + s[1] + thisErrorMsg());
				// currently only supports canBlitz (m_canBlitz)
				if (!s[0].equals("canBlitz"))
					throw new GameParseException("receivesAbilityWhenWith so far only supports: canBlitz" + thisErrorMsg());
			}
		}
		if (!m_whenCombatDamaged.isEmpty())
		{
			for (final Tuple<Tuple<Integer, Integer>, Tuple<String, String>> key : m_whenCombatDamaged)
			{
				final String obj = key.getSecond().getFirst();
				if (obj.equals(UNITSMAYNOTLANDONCARRIER))
					continue;
				if (obj.equals(UNITSMAYNOTLEAVEALLIEDCARRIER))
					continue;
				throw new GameParseException("m_whenCombatDamaged so far only supports: " + UNITSMAYNOTLANDONCARRIER + ", " + UNITSMAYNOTLEAVEALLIEDCARRIER + thisErrorMsg());
			}
		}
	}
	
	public Collection<UnitType> getListedUnits(final String[] list)
	{
		final List<UnitType> rVal = new ArrayList<UnitType>();
		for (final String name : list)
		{
			// Validate all units exist
			final UnitType ut = getData().getUnitTypeList().getUnitType(name);
			if (ut == null)
				throw new IllegalStateException("No unit called: " + name + thisErrorMsg());
			rVal.add(ut);
		}
		return rVal;
	}
	
	public Collection<Territory> getListedTerritories(final String[] list) throws GameParseException
	{
		final List<Territory> rVal = new ArrayList<Territory>();
		for (final String name : list)
		{
			// Validate all territories exist
			final Territory territory = getData().getMap().getTerritory(name);
			if (territory == null)
				throw new GameParseException("No territory called: " + name + thisErrorMsg());
			rVal.add(territory);
		}
		return rVal;
	}
	
	/*private boolean isWW2V3TechModel(final GameData data)
	{
		return games.strategy.triplea.Properties.getWW2V3TechModel(data);
	}*/

	private boolean playerHasRockets(final PlayerID player)
	{
		final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.getRocket();
	}
	
	private boolean playerHasMechInf(final PlayerID player)
	{
		final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.getMechanizedInfantry();
	}
	
	private boolean playerHasParatroopers(final PlayerID player)
	{
		final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.getParatroopers();
	}
	
	@Override
	public String toString()
	{
		// Any overriding method for toString on an attachment needs to include at least the Class, m_attachedTo, and m_name. Or call super.toString()
		return super.toString();
	}
	
	public String allUnitStatsForExporter()
	{
		// should cover ALL fields stored in UnitAttachment
		// remember to test for null and fix arrays
		// the stats exporter relies on this toString having two spaces after each entry, so do not change this please, except to add new abilities onto the end
		return this.getAttachedTo().toString().replaceFirst("games.strategy.engine.data.", "") + " with:"
					+ "  isAir:" + m_isAir
					+ "  isSea:" + m_isSea
					+ "  movement:" + m_movement
					+ "  attack:" + m_attack
					+ "  defense:" + m_defense
					+ "  isTwoHit:" + m_isTwoHit
					+ "  isFactory:" + m_isFactory
					+ "  canBlitz:" + m_canBlitz
					+ "  artillerySupportable:" + m_artillerySupportable
					+ "  artillery:" + m_artillery
					+ "  unitSupportCount:" + m_unitSupportCount
					+ "  attackRolls:" + m_attackRolls
					+ "  defenseRolls:" + m_defenseRolls
					+ "  chooseBestRoll:" + m_chooseBestRoll
					+ "  isMarine:" + m_isMarine
					+ "  isInfantry:" + m_isInfantry
					+ "  isLandTransport:" + m_isLandTransport
					+ "  isAirTransportable:" + m_isAirTransportable
					+ "  isAirTransport:" + m_isAirTransport
					+ "  isStrategicBomber:" + m_isStrategicBomber
					+ "  transportCapacity:" + m_transportCapacity
					+ "  transportCost:" + m_transportCost
					+ "  carrierCapacity:" + m_carrierCapacity
					+ "  carrierCost:" + m_carrierCost
					+ "  isSub:" + m_isSub
					+ "  isDestroyer:" + m_isDestroyer
					+ "  canBombard:" + m_canBombard
					+ "  bombard:" + m_bombard

					+ "  isAAforCombatOnly:" + m_isAAforCombatOnly
					+ "  isAAforBombingThisUnitOnly:" + m_isAAforBombingThisUnitOnly
					+ "  isAAforFlyOverOnly:" + m_isAAforFlyOverOnly
					+ "  attackAA:" + m_attackAA
					+ "  attackAAmaxDieSides:" + m_attackAAmaxDieSides
					+ "  maxAAattacks:" + m_maxAAattacks
					+ "  mayOverStackAA:" + m_mayOverStackAA
					+ "  typeAA:" + m_typeAA
					+ "  targetsAA:" + (m_targetsAA != null ? (m_targetsAA.size() == 0 ? "empty" : m_targetsAA.toString()) : "all air units")
					+ "  willNotFireIfPresent:" + (m_willNotFireIfPresent != null ? (m_willNotFireIfPresent.size() == 0 ? "empty" : m_willNotFireIfPresent.toString()) : "null")
					+ "  isRocket:" + m_isRocket

					+ "  canProduceUnits:" + m_canProduceUnits
					+ "  canProduceXUnits:" + m_canProduceXUnits
					+ "  createsUnitsList:" + (m_createsUnitsList != null ? (m_createsUnitsList.size() == 0 ? "empty" : m_createsUnitsList.toString()) : "null")
					+ "  createsResourcesList:" + (m_createsResourcesList != null ? (m_createsResourcesList.size() == 0 ? "empty" : m_createsResourcesList.toString()) : "null")
					+ "  fuelCost:" + (m_fuelCost != null ? (m_fuelCost.size() == 0 ? "empty" : m_fuelCost.toString()) : "null")
					+ "  isInfrastructure:" + m_isInfrastructure
					+ "  isConstruction:" + m_isConstruction
					+ "  constructionType:" + m_constructionType
					+ "  constructionsPerTerrPerTypePerTurn:" + m_constructionsPerTerrPerTypePerTurn
					+ "  maxConstructionsPerTypePerTerr:" + m_maxConstructionsPerTypePerTerr
					+ "  destroyedWhenCapturedBy:" + (m_destroyedWhenCapturedBy != null ? (m_destroyedWhenCapturedBy.size() == 0 ? "empty" : m_destroyedWhenCapturedBy.toString()) : "null")
					+ "  canBeCapturedOnEnteringBy:" + (m_canBeCapturedOnEnteringBy != null ? (m_canBeCapturedOnEnteringBy.size() == 0 ? "empty" : m_canBeCapturedOnEnteringBy.toString()) : "null")
					+ "  canBeDamaged:" + m_canBeDamaged
					+ "  canDieFromReachingMaxDamage:" + m_canDieFromReachingMaxDamage
					+ "  maxOperationalDamage:" + m_maxOperationalDamage
					+ "  maxDamage:" + m_maxDamage
					+ "  unitPlacementRestrictions:"
					+ (m_unitPlacementRestrictions != null ? (m_unitPlacementRestrictions.length == 0 ? "empty" : Arrays.toString(m_unitPlacementRestrictions)) : "null")
					+ "  requiresUnits:" + (m_requiresUnits != null ? (m_requiresUnits.size() == 0 ? "empty" : MyFormatter.listOfArraysToString(m_requiresUnits)) : "null")
					+ "  consumesUnits:" + (m_consumesUnits != null ? (m_consumesUnits.size() == 0 ? "empty" : m_consumesUnits.toString()) : "null")
					+ "  canOnlyBePlacedInTerritoryValuedAtX:" + m_canOnlyBePlacedInTerritoryValuedAtX
					+ "  maxBuiltPerPlayer:" + m_maxBuiltPerPlayer
					+ "  special:" + (m_special != null ? (m_special.size() == 0 ? "empty" : m_special.toString()) : "null")
					+ "  isSuicide:" + m_isSuicide
					+ "  isSuicide:" + m_isSuicide
					+ "  isCombatTransport:" + m_isCombatTransport
					+ "  canInvadeOnlyFrom:" + (m_canInvadeOnlyFrom != null ? (m_canInvadeOnlyFrom.length == 0 ? "empty" : Arrays.toString(m_canInvadeOnlyFrom)) : "null")
					+ "  canBeGivenByTerritoryTo:" + (m_canBeGivenByTerritoryTo != null ? (m_canBeGivenByTerritoryTo.size() == 0 ? "empty" : m_canBeGivenByTerritoryTo.toString()) : "null")
					+ "  receivesAbilityWhenWith:" + (m_receivesAbilityWhenWith != null ? (m_receivesAbilityWhenWith.size() == 0 ? "empty" : m_receivesAbilityWhenWith.toString()) : "null")
					+ "  whenCombatDamaged:" + (m_whenCombatDamaged != null ? (m_whenCombatDamaged.size() == 0 ? "empty" : m_whenCombatDamaged.toString()) : "null")
					+ "  blockade:" + m_blockade
					+ "  bombingMaxDieSides:" + m_bombingMaxDieSides
					+ "  bombingBonus:" + m_bombingBonus
					+ "  bombingTargets:" + m_bombingTargets
					+ "  givesMovement:" + (m_givesMovement != null ? (m_givesMovement.size() == 0 ? "empty" : m_givesMovement.toString()) : "null")
					+ "  repairsUnits:" + (m_repairsUnits != null ? (m_repairsUnits.length == 0 ? "empty" : Arrays.toString(m_repairsUnits)) : "null")
					+ "  canScramble:" + m_canScramble
					+ "  maxScrambleDistance:" + m_maxScrambleDistance
					+ "  isAirBase:" + m_isAirBase
					+ "  maxScrambleCount:" + m_maxScrambleCount
					+ "  whenCapturedChangesInto:" + (m_whenCapturedChangesInto != null ? (m_whenCapturedChangesInto.size() == 0 ? "empty" : m_whenCapturedChangesInto.toString()) : "null")
					+ "  canIntercept:" + m_canIntercept
					+ "  canEscort:" + m_canEscort
					+ "  airDefense:" + m_airDefense
					+ "  airAttack:" + m_airAttack
					+ "  canNotMoveDuringCombatMove:" + m_canNotMoveDuringCombatMove
					+ "  movementLimit:" + (m_movementLimit != null ? m_movementLimit.toString() : "null")
					+ "  attackingLimit:" + (m_attackingLimit != null ? m_attackingLimit.toString() : "null")
					+ "  placementLimit:" + (m_placementLimit != null ? m_placementLimit.toString() : "null");
	}
	
	public String toStringShortAndOnlyImportantDifferences(final PlayerID player, final boolean useHTML, final boolean includeAttachedToName)
	{
		// displays everything in a very short form, in English rather than as xml stuff
		// shows all except for: m_constructionType, m_constructionsPerTerrPerTypePerTurn, m_maxConstructionsPerTypePerTerr, m_canBeGivenByTerritoryTo, m_destroyedWhenCapturedBy, m_canBeCapturedOnEnteringBy
		final StringBuilder stats = new StringBuilder();
		// if (this != null && this.getName() != null)
		// stats.append(this.getName() + ": ");
		if (includeAttachedToName && this != null && this.getAttachedTo() != null)
			stats.append(this.getAttachedTo().toString().replaceFirst("games.strategy.engine.data.", "") + ", ");
		if (m_isAir)
			stats.append("Air unit, ");
		else if (m_isSea)
			stats.append("Sea unit, ");
		else
			stats.append("Land unit, ");
		if (getAttack(player) > 0)
			stats.append((getAttackRolls(player) > 1 ? (getAttackRolls(player) + "x ") : "") + getAttack(player) + " Attack, ");
		if (getDefense(player) > 0)
			stats.append((getDefenseRolls(player) > 1 ? (getDefenseRolls(player) + "x ") : "") + getDefense(player) + " Defense, ");
		if (getMovement(player) > 0)
			stats.append(getMovement(player) + " Movement, ");
		if (m_isTwoHit)
			stats.append("Two Hitpoints, ");
		if ((m_isFactory || m_canProduceUnits) && m_canProduceXUnits < 0)
			stats.append("can Produce Units Up To Territory Value, ");
		else if ((m_isFactory || m_canProduceUnits) && m_canProduceXUnits > 0)
			stats.append("can Produce " + m_canProduceXUnits + " Units, ");
		if (m_createsUnitsList != null && m_createsUnitsList.size() == 1)
			stats.append("Produces " + m_createsUnitsList.totalValues() + " " + m_createsUnitsList.keySet().iterator().next().getName() + " Each Turn, ");
		else if (m_createsUnitsList != null && m_createsUnitsList.size() > 1)
			stats.append("Produces " + m_createsUnitsList.totalValues() + " Units Each Turn, ");
		if (m_createsResourcesList != null && m_createsResourcesList.size() == 1)
			stats.append("Produces " + m_createsResourcesList.totalValues() + " " + m_createsResourcesList.keySet().iterator().next().getName() + " Each Turn, ");
		else if (m_createsResourcesList != null && m_createsResourcesList.size() > 1)
			stats.append("Produces " + m_createsResourcesList.totalValues() + " Resources Each Turn, ");
		
		if (m_fuelCost != null && m_fuelCost.size() == 1)
			stats.append("Uses " + m_fuelCost.totalValues() + " " + m_fuelCost.keySet().iterator().next().getName() + " Each movement point, ");
		else if (m_fuelCost != null && m_fuelCost.size() > 1)
			stats.append("Uses " + m_fuelCost.totalValues() + " Resources Each movement point, ");
		
		if (m_isAAforCombatOnly || m_isAAforBombingThisUnitOnly || m_isAAforFlyOverOnly)
		{
			stats.append(this.getAttackAA(player) + "/" + (m_attackAAmaxDieSides != -1 ? m_attackAAmaxDieSides : getData().getDiceSides()) + " ");
			if (m_isAAforCombatOnly && m_isAAforBombingThisUnitOnly && m_isAAforFlyOverOnly)
				stats.append("Anti-Air, ");
			else if (m_isAAforCombatOnly && m_isAAforFlyOverOnly && !games.strategy.triplea.Properties.getAATerritoryRestricted(getData()))
				stats.append("Anti-Air for Combat & FlyOver, ");
			else if (m_isAAforBombingThisUnitOnly && m_isAAforFlyOverOnly && !games.strategy.triplea.Properties.getAATerritoryRestricted(getData()))
				stats.append("Anti-Air for Raids & FlyOver, ");
			else if (m_isAAforCombatOnly)
				stats.append("Anti-Air for Combat, ");
			else if (m_isAAforBombingThisUnitOnly)
				stats.append("Anti-Air for Raids, ");
			else if (m_isAAforFlyOverOnly)
				stats.append("Anti-Air for FlyOver, ");
			if (m_maxAAattacks > -1)
				stats.append(m_maxAAattacks + " AA Attacks");
		}
		if (m_isRocket && playerHasRockets(player))
		{
			stats.append("can Rocket Attack, ");
			final int bombingBonus = getBombingBonus();
			if ((m_bombingMaxDieSides != -1 || bombingBonus != -1) && games.strategy.triplea.Properties.getUseBombingMaxDiceSidesAndBonus(getData()))
				stats.append((bombingBonus != -1 ? bombingBonus + 1 : 1)
							+ "-"
							+ (m_bombingMaxDieSides != -1 ? m_bombingMaxDieSides + (bombingBonus != -1 ? bombingBonus : 0) : getData().getDiceSides() + (bombingBonus != -1 ? bombingBonus : 0))
							+ " Rocket Damage, ");
			else
				stats.append("1-" + getData().getDiceSides() + " Rocket Damage, ");
		}
		// line break
		if (useHTML)
			stats.append("<br /> &nbsp;&nbsp;&nbsp;&nbsp; ");
		if (m_isInfrastructure || m_isFactory)
			stats.append("can be Captured, ");
		if (m_isConstruction || m_isFactory)
			stats.append("can be Placed Without Factory, ");
		if ((m_canBeDamaged || m_isFactory) && games.strategy.triplea.Properties.getSBRAffectsUnitProduction(getData()))
		{
			stats.append("can be Damaged By Raids, ");
			if (m_canDieFromReachingMaxDamage)
				stats.append("will Die If Max Damage Reached, ");
		}
		else if ((m_canBeDamaged || m_isFactory) && games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(getData()))
		{
			stats.append("can be Damaged By Raids, ");
			if (m_maxOperationalDamage > -1)
				stats.append(m_maxOperationalDamage + " Max Operational Damage, ");
			if ((m_canProduceUnits || m_isFactory) && m_canProduceXUnits < 0)
				stats.append("Total Damage up to " + (m_maxDamage > -1 ? m_maxDamage : 2) + "x Territory Value, ");
			else if (m_maxDamage > -1)
				stats.append(m_maxDamage + " Max Total Damage, ");
			if (m_canDieFromReachingMaxDamage)
				stats.append("will Die If Max Damage Reached, ");
		}
		else if (m_canBeDamaged || m_isFactory)
			stats.append("can be Attacked By Raids, ");
		if (m_isAirBase && games.strategy.triplea.Properties.getScramble_Rules_In_Effect(getData()))
			stats.append("can Allow Scrambling, ");
		if (m_canScramble && games.strategy.triplea.Properties.getScramble_Rules_In_Effect(getData()))
			stats.append("can Scramble " + (m_maxScrambleDistance > 0 ? m_maxScrambleDistance : 1) + " Distance, ");
		if (m_artillery)
			stats.append("can Give Attack Bonus, ");
		// TODO: Need to account for support attachments here somehow.
		if (m_artillerySupportable)
			stats.append("can Receive Attack Bonus, ");
		if (m_isMarine)
			stats.append("1" + " Amphibious Attack Bonus, ");
		if (getCanBlitz(player))
			stats.append("can Blitz, ");
		if (!m_receivesAbilityWhenWith.isEmpty())
		{
			if (m_receivesAbilityWhenWith.size() == 1)
				stats.append("receives " + m_receivesAbilityWhenWith.get(0).split(":")[0] + " when paired with " + m_receivesAbilityWhenWith.get(0).split(":")[1] + ", ");
			else
				stats.append("receives abilities when paired with other units, ");
		}
		// TODO: Need to account for dice rolls, once we can customize dice rolls allowed per unit
		if (m_isStrategicBomber)
		{
			stats.append("can Perform Raids, ");
			final int bombingBonus = getBombingBonus();
			if ((m_bombingMaxDieSides != -1 || bombingBonus != -1) && games.strategy.triplea.Properties.getUseBombingMaxDiceSidesAndBonus(getData()))
				stats.append((bombingBonus != -1 ? bombingBonus + 1 : 1)
							+ "-"
							+ (m_bombingMaxDieSides != -1 ? m_bombingMaxDieSides + (bombingBonus != -1 ? bombingBonus : 0) : getData().getDiceSides() + (bombingBonus != -1 ? bombingBonus : 0))
							+ " Raid Damage, ");
			else
				stats.append("1-" + getData().getDiceSides() + " Raid Damage, ");
		}
		if (m_isSub)
			stats.append("is Stealth, ");
		if (m_isDestroyer)
			stats.append("is Anti-Stealth, ");
		if (getCanBombard(player) && getBombard(player) > 0)
			stats.append(getBombard(player) + " Bombard, ");
		if (m_blockade > 0)
			stats.append(m_blockade + " Blockade Loss, ");
		if (m_isSuicide)
			stats.append("Suicide/Munition Unit, ");
		if (m_isAir && (m_isKamikaze || games.strategy.triplea.Properties.getKamikaze_Airplanes(getData())))
			stats.append("can use All Movement To Attack Target, ");
		if (m_isInfantry && playerHasMechInf(player))
			stats.append("can be Transported By Land, ");
		if (m_isLandTransport && playerHasMechInf(player))
			stats.append("is a Land Transport, ");
		if (m_isAirTransportable && playerHasParatroopers(player))
			stats.append("can be Transported By Air, ");
		if (m_isAirTransport && playerHasParatroopers(player))
			stats.append("is an Air Transport, ");
		if (m_isCombatTransport && m_transportCapacity > 0)
			stats.append("is a Combat Transport, ");
		else if (m_transportCapacity > 0 && m_isSea)
			stats.append("is a Sea Transport, ");
		if (m_transportCost > -1)
			stats.append(m_transportCost + " Transporting Cost, ");
		if (m_transportCapacity > 0 && m_isSea)
			stats.append(m_transportCapacity + " Transporting Capacity, ");
		else if (m_transportCapacity > 0 && m_isAir && playerHasParatroopers(player))
			stats.append(m_transportCapacity + " Transporting Capacity, ");
		else if (m_transportCapacity > 0 && playerHasMechInf(player) && !m_isSea && !m_isAir)
			stats.append(m_transportCapacity + " Transporting Capacity, ");
		if (m_carrierCost > -1)
			stats.append(m_carrierCost + " Carrier Cost, ");
		if (m_carrierCapacity > 0)
			stats.append(m_carrierCapacity + " Carrier Capacity, ");
		if (!m_whenCombatDamaged.isEmpty())
		{
			stats.append("when hit this unit loses certain abilities, ");
		}
		// line break
		if (useHTML)
			stats.append("<br /> &nbsp;&nbsp;&nbsp;&nbsp; ");
		if (m_maxBuiltPerPlayer > -1)
			stats.append(m_maxBuiltPerPlayer + " Max Built Allowed, ");
		if (m_repairsUnits != null && games.strategy.triplea.Properties.getTwoHitPointUnitsRequireRepairFacilities(getData())
					&& (games.strategy.triplea.Properties.getBattleshipsRepairAtBeginningOfRound(getData()) || games.strategy.triplea.Properties.getBattleshipsRepairAtEndOfRound(getData())))
			stats.append("can Repair Some Units, ");
		if (m_givesMovement != null && m_givesMovement.totalValues() > 0 && games.strategy.triplea.Properties.getUnitsMayGiveBonusMovement(getData()))
			stats.append("can Give Bonus Movement, ");
		else if (m_givesMovement != null && m_givesMovement.totalValues() < 0 && games.strategy.triplea.Properties.getUnitsMayGiveBonusMovement(getData()))
			stats.append("can Take Away Movement, ");
		if (m_consumesUnits != null && m_consumesUnits.totalValues() == 1)
			stats.append("unit is an Upgrade Of " + m_consumesUnits.keySet().iterator().next().getName() + ", ");
		else if (m_consumesUnits != null && m_consumesUnits.totalValues() > 1)
			stats.append("unit Consumes Other Units On Placement, ");
		if (m_requiresUnits != null && m_requiresUnits.size() == 1 && m_requiresUnits.iterator().next().length == 1 && games.strategy.triplea.Properties.getUnitPlacementRestrictions(getData()))
			stats.append("unit can only be Placed Where There Is A " + m_requiresUnits.iterator().next()[0] + ", ");
		else if (m_requiresUnits != null && m_requiresUnits.size() > 0 && games.strategy.triplea.Properties.getUnitPlacementRestrictions(getData()))
			stats.append("unit Requires Other Units Present To Be Placed, ");
		if (m_unitPlacementRestrictions != null && games.strategy.triplea.Properties.getUnitPlacementRestrictions(getData()))
			stats.append("has Placement Restrictions, ");
		if (m_canOnlyBePlacedInTerritoryValuedAtX > 0 && games.strategy.triplea.Properties.getUnitPlacementRestrictions(getData()))
			stats.append("must be Placed In Territory Valued >=" + m_canOnlyBePlacedInTerritoryValuedAtX + ", ");
		if (m_canNotMoveDuringCombatMove)
			stats.append("cannot Combat Move, ");
		if (m_movementLimit != null)
		{
			if (m_movementLimit.getFirst() == Integer.MAX_VALUE
						&& (m_isAAforBombingThisUnitOnly || m_isAAforCombatOnly)
						&& !(games.strategy.triplea.Properties.getWW2V2(getData()) || games.strategy.triplea.Properties.getWW2V3(getData()) || games.strategy.triplea.Properties
									.getMultipleAAPerTerritory(getData())))
				stats.append("max of 1 " + m_movementLimit.getSecond() + " moving per territory, ");
			else if (m_movementLimit.getFirst() < 10000)
				stats.append("max of " + m_movementLimit.getFirst() + " " + m_movementLimit.getSecond() + " moving per territory, ");
		}
		if (m_attackingLimit != null)
		{
			if (m_attackingLimit.getFirst() == Integer.MAX_VALUE
						&& (m_isAAforBombingThisUnitOnly || m_isAAforCombatOnly)
						&& !(games.strategy.triplea.Properties.getWW2V2(getData()) || games.strategy.triplea.Properties.getWW2V3(getData()) || games.strategy.triplea.Properties
									.getMultipleAAPerTerritory(getData())))
				stats.append("max of 1 " + m_attackingLimit.getSecond() + " attacking per territory, ");
			else if (m_attackingLimit.getFirst() < 10000)
				stats.append("max of " + m_attackingLimit.getFirst() + " " + m_attackingLimit.getSecond() + " attacking per territory, ");
		}
		if (m_placementLimit != null)
		{
			if (m_placementLimit.getFirst() == Integer.MAX_VALUE
						&& (m_isAAforBombingThisUnitOnly || m_isAAforCombatOnly)
						&& !(games.strategy.triplea.Properties.getWW2V2(getData()) || games.strategy.triplea.Properties.getWW2V3(getData()) || games.strategy.triplea.Properties
									.getMultipleAAPerTerritory(getData())))
				stats.append("max of 1 " + m_placementLimit.getSecond() + " placed per territory, ");
			else if (m_placementLimit.getFirst() < 10000)
				stats.append("max of " + m_placementLimit.getFirst() + " " + m_placementLimit.getSecond() + " placed per territory, ");
		}
		
		if (stats.indexOf(", ") > -1)
			stats.delete(stats.lastIndexOf(", "), stats.length() - 1);
		return stats.toString();
	}
	
	/** does nothing, kept to avoid breaking maps, do not remove */
	@Deprecated
	@GameProperty(xmlProperty = true, gameProperty = false, adds = false)
	public void setIsParatroop(final String s)
	{
	}
	
	/** does nothing, used to keep compatibility with older xml files, do not remove */
	@Deprecated
	@GameProperty(xmlProperty = true, gameProperty = false, adds = false)
	public void setIsMechanized(final String s)
	{
	}
	
}
