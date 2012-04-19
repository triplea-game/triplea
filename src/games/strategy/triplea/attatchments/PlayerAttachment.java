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
 * PlayerAttachment.java
 * 
 * Created on August 29, 2005, 3:14 PM
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
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * 
 * @author Adam Jette, and Mark Christopher Duncan
 * 
 * @version 1.0
 */
public class PlayerAttachment extends DefaultAttachment
{
	private static final long serialVersionUID = 1880755875866426270L;
	
	/**
	 * Convenience method. can be null
	 */
	public static PlayerAttachment get(final PlayerID p)
	{
		final PlayerAttachment rVal = (PlayerAttachment) p.getAttachment(Constants.PLAYER_ATTACHMENT_NAME);
		// allow null
		return rVal;
	}
	
	public static PlayerAttachment get(final PlayerID p, final String nameOfAttachment)
	{
		final PlayerAttachment rVal = (PlayerAttachment) p.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("No player attachment for:" + p.getName() + " with name:" + nameOfAttachment);
		return rVal;
	}
	
	private int m_vps = 0;
	private int m_captureVps = 0; // need to store some data during a turn
	private int m_retainCapitalNumber = 1; // number of capitals needed before we lose all our money
	private int m_retainCapitalProduceNumber = 1; // number of capitals needed before we lose ability to gain money and produce units
	private ArrayList<PlayerID> m_giveUnitControl = new ArrayList<PlayerID>();
	private ArrayList<PlayerID> m_captureUnitOnEnteringBy = new ArrayList<PlayerID>();
	private boolean m_destroysPUs = false; // do we lose our money and have it disappear or is that money captured?
	private IntegerMap<Resource> m_suicideAttackResources = new IntegerMap<Resource>(); // what resources can be used for suicide attacks, and at what attack power
	private HashSet<UnitType> m_suicideAttackTargets = null; // what can be hit by suicide attacks
	private HashSet<Triple<Integer, String, HashSet<UnitType>>> m_placementLimit = new HashSet<Triple<Integer, String, HashSet<UnitType>>>(); // placement limits on a flexible per player basis
	private HashSet<Triple<Integer, String, HashSet<UnitType>>> m_movementLimit = new HashSet<Triple<Integer, String, HashSet<UnitType>>>(); // movement limits on a flexible per player basis
	private HashSet<Triple<Integer, String, HashSet<UnitType>>> m_attackingLimit = new HashSet<Triple<Integer, String, HashSet<UnitType>>>(); // attacking number limits on a flexible per player basis
	
	/** Creates new PlayerAttachment */
	public PlayerAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setPlacementLimit(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length < 3)
			throw new GameParseException("placementLimit must have 3 parts: count, type, unit list" + thisErrorMsg());
		final int max = getInt(s[0]);
		if (max < 0)
			throw new GameParseException("placementLimit count must have a positive number" + thisErrorMsg());
		if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total")))
			throw new GameParseException("placementLimit type must be: owned, allied, or total" + thisErrorMsg());
		final HashSet<UnitType> types = new HashSet<UnitType>();
		if (s[3].equalsIgnoreCase("all"))
			types.addAll(getData().getUnitTypeList().getAllUnitTypes());
		else
		{
			for (int i = 2; i < s.length; i++)
			{
				final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
				if (ut == null)
					throw new GameParseException("No unit called: " + s[i] + thisErrorMsg());
				else
					types.add(ut);
			}
		}
		m_placementLimit.add(new Triple<Integer, String, HashSet<UnitType>>(max, s[1], types));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setPlacementLimit(final HashSet<Triple<Integer, String, HashSet<UnitType>>> value)
	{
		m_placementLimit = value;
	}
	
	public HashSet<Triple<Integer, String, HashSet<UnitType>>> getPlacementLimit()
	{
		return m_placementLimit;
	}
	
	public void clearPlacementLimit()
	{
		m_placementLimit.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setMovementLimit(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length < 3)
			throw new GameParseException("movementLimit must have 3 parts: count, type, unit list" + thisErrorMsg());
		final int max = getInt(s[0]);
		if (max < 0)
			throw new GameParseException("movementLimit count must have a positive number" + thisErrorMsg());
		if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total")))
			throw new GameParseException("movementLimit type must be: owned, allied, or total" + thisErrorMsg());
		final HashSet<UnitType> types = new HashSet<UnitType>();
		if (s[3].equalsIgnoreCase("all"))
			types.addAll(getData().getUnitTypeList().getAllUnitTypes());
		else
		{
			for (int i = 2; i < s.length; i++)
			{
				final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
				if (ut == null)
					throw new GameParseException("No unit called: " + s[i] + thisErrorMsg());
				else
					types.add(ut);
			}
		}
		m_movementLimit.add(new Triple<Integer, String, HashSet<UnitType>>(max, s[1], types));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMovementLimit(final HashSet<Triple<Integer, String, HashSet<UnitType>>> value)
	{
		m_movementLimit = value;
	}
	
	public HashSet<Triple<Integer, String, HashSet<UnitType>>> getMovementLimit()
	{
		return m_movementLimit;
	}
	
	public void clearMovementLimit()
	{
		m_movementLimit.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setAttackingLimit(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length < 3)
			throw new GameParseException("attackingLimit must have 3 parts: count, type, unit list" + thisErrorMsg());
		final int max = getInt(s[0]);
		if (max < 0)
			throw new GameParseException("attackingLimit count must have a positive number" + thisErrorMsg());
		if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total")))
			throw new GameParseException("attackingLimit type must be: owned, allied, or total" + thisErrorMsg());
		final HashSet<UnitType> types = new HashSet<UnitType>();
		if (s[3].equalsIgnoreCase("all"))
			types.addAll(getData().getUnitTypeList().getAllUnitTypes());
		else
		{
			for (int i = 2; i < s.length; i++)
			{
				final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
				if (ut == null)
					throw new GameParseException("No unit called: " + s[i] + thisErrorMsg());
				else
					types.add(ut);
			}
		}
		m_attackingLimit.add(new Triple<Integer, String, HashSet<UnitType>>(max, s[1], types));
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttackingLimit(final HashSet<Triple<Integer, String, HashSet<UnitType>>> value)
	{
		m_attackingLimit = value;
	}
	
	public HashSet<Triple<Integer, String, HashSet<UnitType>>> getAttackingLimit()
	{
		return m_attackingLimit;
	}
	
	public void clearAttackingLimit()
	{
		m_attackingLimit.clear();
	}
	
	public static boolean getCanTheseUnitsMoveWithoutViolatingStackingLimit(final String limitType, final Collection<Unit> unitsMoving, final Territory toMoveInto, final PlayerID owner,
				final GameData data)
	{
		final PlayerAttachment pa = PlayerAttachment.get(owner);
		if (pa == null)
			return true;
		final HashSet<Triple<Integer, String, HashSet<UnitType>>> stackingLimits;
		if (limitType.equals("movementLimit"))
			stackingLimits = pa.getMovementLimit();
		else if (limitType.equals("attackingLimit"))
			stackingLimits = pa.getAttackingLimit();
		else if (limitType.equals("placementLimit"))
			stackingLimits = pa.getPlacementLimit();
		else
			throw new IllegalStateException("getCanTheseUnitsMoveWithoutViolatingStackingLimit does not allow limitType: " + limitType);
		if (stackingLimits.isEmpty())
			return true;
		for (final Triple<Integer, String, HashSet<UnitType>> currentLimit : stackingLimits)
		{
			// first make a copy of unitsMoving
			final Collection<Unit> copyUnitsMoving = new ArrayList<Unit>(unitsMoving);
			final int max = currentLimit.getFirst();
			final String type = currentLimit.getSecond();
			final HashSet<UnitType> unitsToTest = currentLimit.getThird();
			final Collection<Unit> currentInTerritory = toMoveInto.getUnits().getUnits();
			// first remove units that do not apply to our current type
			if (type.equals("owned"))
			{
				currentInTerritory.removeAll(Match.getMatches(currentInTerritory, Matches.unitIsOwnedBy(owner).invert()));
				copyUnitsMoving.removeAll(Match.getMatches(copyUnitsMoving, Matches.unitIsOwnedBy(owner).invert()));
			}
			else if (type.equals("allied"))
			{
				currentInTerritory.removeAll(Match.getMatches(currentInTerritory, Matches.alliedUnit(owner, data).invert()));
				copyUnitsMoving.removeAll(Match.getMatches(copyUnitsMoving, Matches.alliedUnit(owner, data).invert()));
			}
			// else if (type.equals("total"))
			// now remove units that are not part of our list
			currentInTerritory.retainAll(Match.getMatches(currentInTerritory, Matches.unitIsOfTypes(unitsToTest)));
			copyUnitsMoving.retainAll(Match.getMatches(copyUnitsMoving, Matches.unitIsOfTypes(unitsToTest)));
			// now test
			if (max < (currentInTerritory.size() + copyUnitsMoving.size()))
				return false;
		}
		return true;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setSuicideAttackTargets(final String value) throws GameParseException
	{
		if (value == null)
		{
			m_suicideAttackTargets = null;
			return;
		}
		if (m_suicideAttackTargets == null)
			m_suicideAttackTargets = new HashSet<UnitType>();
		final String[] s = value.split(":");
		for (final String u : s)
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(u);
			if (ut == null)
				throw new GameParseException("suicideAttackTargets: no such unit called " + u + thisErrorMsg());
			m_suicideAttackTargets.add(ut);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setSuicideAttackTargets(final HashSet<UnitType> value)
	{
		m_suicideAttackTargets = value;
	}
	
	public HashSet<UnitType> getSuicideAttackTargets()
	{
		return m_suicideAttackTargets;
	}
	
	public void clearSuicideAttackTargets()
	{
		m_suicideAttackTargets.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setSuicideAttackResources(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("suicideAttackResources must have exactly 2 fields" + thisErrorMsg());
		final int attackValue = getInt(s[0]);
		if (attackValue < 0)
			throw new GameParseException("suicideAttackResources attack value must be positive" + thisErrorMsg());
		final Resource r = getData().getResourceList().getResource(s[1]);
		if (r == null)
			throw new GameParseException("no such resource: " + s[1] + thisErrorMsg());
		m_suicideAttackResources.put(r, attackValue);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setSuicideAttackResources(final IntegerMap<Resource> value)
	{
		m_suicideAttackResources = value;
	}
	
	public IntegerMap<Resource> getSuicideAttackResources()
	{
		return m_suicideAttackResources;
	}
	
	public void clearSuicideAttackResources()
	{
		m_suicideAttackResources.clear();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setVps(final String value)
	{
		m_vps = getInt(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setVps(final Integer value)
	{
		m_vps = value;
	}
	
	public int getVps()
	{
		return m_vps;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCaptureVps(final String value)
	{
		m_captureVps = getInt(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCaptureVps(final Integer value)
	{
		m_captureVps = value;
	}
	
	public int getCaptureVps()
	{
		return m_captureVps;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRetainCapitalNumber(final String value)
	{
		m_retainCapitalNumber = getInt(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRetainCapitalNumber(final Integer value)
	{
		m_retainCapitalNumber = value;
	}
	
	public int getRetainCapitalNumber()
	{
		return m_retainCapitalNumber;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRetainCapitalProduceNumber(final String value)
	{
		m_retainCapitalProduceNumber = getInt(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRetainCapitalProduceNumber(final Integer value)
	{
		m_retainCapitalProduceNumber = value;
	}
	
	public int getRetainCapitalProduceNumber()
	{
		return m_retainCapitalProduceNumber;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setGiveUnitControl(final String value) throws GameParseException
	{
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_giveUnitControl.add(tempPlayer);
			else if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false"))
				m_giveUnitControl.clear();
			else
				throw new GameParseException("No player named: " + name + thisErrorMsg());
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setGiveUnitControl(final ArrayList<PlayerID> value)
	{
		m_giveUnitControl = value;
	}
	
	public ArrayList<PlayerID> getGiveUnitControl()
	{
		return m_giveUnitControl;
	}
	
	public void clearGiveUnitControl()
	{
		m_giveUnitControl.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setCaptureUnitOnEnteringBy(final String value) throws GameParseException
	{
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_captureUnitOnEnteringBy.add(tempPlayer);
			else
				throw new GameParseException("No player named: " + name + thisErrorMsg());
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCaptureUnitOnEnteringBy(final ArrayList<PlayerID> value)
	{
		m_captureUnitOnEnteringBy = value;
	}
	
	public ArrayList<PlayerID> getCaptureUnitOnEnteringBy()
	{
		return m_captureUnitOnEnteringBy;
	}
	
	public void clearCaptureUnitOnEnteringBy()
	{
		m_captureUnitOnEnteringBy.clear();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDestroysPUs(final String value)
	{
		m_destroysPUs = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDestroysPUs(final Boolean value)
	{
		m_destroysPUs = value;
	}
	
	public boolean getDestroysPUs()
	{
		return m_destroysPUs;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
	
	/** setTakeUnitControl (and getTakeUnitControl) DO NOTHING. They are kept for backwards compatibility only, otherwise users get Java errors. */
	@Deprecated
	@GameProperty(xmlProperty = true, gameProperty = false, adds = false)
	public void setTakeUnitControl(final String value)
	{
	}
}
