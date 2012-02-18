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
 * TerritoryAttachment.java
 * 
 * Created on November 8, 2001, 3:08 PM
 */
package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;
import games.strategy.triplea.formatter.MyFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
@SuppressWarnings("serial")
public class TerritoryAttachment extends DefaultAttachment
{
	public static Territory getCapital(final PlayerID player, final GameData data)
	{
		for (final Territory current : data.getMap().getTerritories())
		{
			final TerritoryAttachment ta = TerritoryAttachment.get(current);
			if (ta != null && ta.getCapital() != null)
			{
				final PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
				if (whoseCapital == null)
					throw new IllegalStateException("Invalid capital for player name:" + ta.getCapital());
				if (player.equals(whoseCapital))
					return current;
			}
		}
		// Added check for optional players- no error thrown for them
		if (player.getOptional())
			return null;
		throw new IllegalStateException("Capital not found for:" + player);
	}
	
	/**
	 * will return empty list if none controlled, never returns null
	 */
	public static List<Territory> getAllCapitals(final PlayerID player, final GameData data)
	{
		final List<Territory> capitals = new ArrayList<Territory>();
		for (final Territory current : data.getMap().getTerritories())
		{
			final TerritoryAttachment ta = TerritoryAttachment.get(current);
			if (ta != null && ta.getCapital() != null)
			{
				final PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
				if (whoseCapital == null)
					throw new IllegalStateException("Invalid capital for player name:" + ta.getCapital());
				if (player.equals(whoseCapital))
					capitals.add(current);
			}
		}
		if (!capitals.isEmpty())
			return capitals;
		// Added check for optional players- no error thrown for them
		if (player.getOptional())
			return capitals;
		throw new IllegalStateException("Capital not found for:" + player);
	}
	
	/**
	 * will return empty list if none controlled, never returns null
	 */
	public static List<Territory> getAllCurrentlyOwnedCapitals(final PlayerID player, final GameData data)
	{
		final List<Territory> capitals = new ArrayList<Territory>();
		for (final Territory current : data.getMap().getTerritories())
		{
			final TerritoryAttachment ta = TerritoryAttachment.get(current);
			if (ta != null && ta.getCapital() != null)
			{
				final PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
				if (whoseCapital == null)
					throw new IllegalStateException("Invalid capital for player name:" + ta.getCapital());
				if (player.equals(whoseCapital) && player.equals(current.getOwner()))
					capitals.add(current);
			}
		}
		return capitals;
	}
	
	/**
	 * Convenience method. can return null.
	 */
	public static TerritoryAttachment get(final Territory t)
	{
		final TerritoryAttachment rVal = (TerritoryAttachment) t.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
		if (rVal == null && !t.isWater())
			throw new IllegalStateException("No territory attachment for:" + t.getName());
		return rVal;
	}
	
	public static TerritoryAttachment get(final Territory t, final String nameOfAttachment)
	{
		final TerritoryAttachment rVal = (TerritoryAttachment) t.getAttachment(nameOfAttachment);
		if (rVal == null && !t.isWater())
			throw new IllegalStateException("No territory attachment for:" + t.getName() + " with name:" + nameOfAttachment);
		return rVal;
	}
	
	private String m_capital = null;
	private boolean m_originalFactory = false;
	private int m_production = 0; // "setProduction" will set both m_production and m_unitProduction. While "setProductionOnly" sets only m_production.
	private boolean m_victoryCity = false;
	private boolean m_isImpassible = false;
	@InternalDoNotExport
	private PlayerID m_originalOwner = null; // Do Not Export (do not include in IAttachment). This is not set by the xml in territory attachments, but instead set during territory initialization.
	private PlayerID m_occupiedTerrOf = null;
	private boolean m_convoyRoute = false;
	private HashSet<Territory> m_convoyAttached = new HashSet<Territory>();
	private ArrayList<PlayerID> m_changeUnitOwners = new ArrayList<PlayerID>();
	private ArrayList<PlayerID> m_captureUnitOnEnteringBy = new ArrayList<PlayerID>();
	private boolean m_navalBase = false;
	private boolean m_airBase = false;
	private boolean m_kamikazeZone = false;
	private int m_unitProduction = 0;
	private boolean m_blockadeZone = false;
	private ArrayList<TerritoryEffect> m_territoryEffect = new ArrayList<TerritoryEffect>();
	private ArrayList<String> m_whenCapturedByGoesTo = new ArrayList<String>();
	private ResourceCollection m_resources = null;
	
	/** Creates new TerritoryAttachment */
	public TerritoryAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setResources(final String value) throws GameParseException
	{
		if (value == null)
		{
			m_resources = null;
			return;
		}
		if (m_resources == null)
			m_resources = new ResourceCollection(getData());
		final String[] s = value.split(":");
		final int amount = getInt(s[0]);
		if (s[1].equals(Constants.PUS))
			throw new GameParseException("Please set PUs using production, not resource" + thisErrorMsg());
		final Resource resource = getData().getResourceList().getResource(s[1]);
		if (resource == null)
			throw new GameParseException("No resource named: " + s[1] + thisErrorMsg());
		m_resources.putResource(resource, amount);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setResources(final ResourceCollection value)
	{
		m_resources = value;
	}
	
	public ResourceCollection getResources()
	{
		return m_resources;
	}
	
	public void clearResources()
	{
		m_resources = new ResourceCollection(getData());
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsImpassible(final String value)
	{
		m_isImpassible = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsImpassible(final Boolean value)
	{
		m_isImpassible = value;
	}
	
	public boolean getIsImpassible()
	{
		return m_isImpassible;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCapital(final String value) throws GameParseException
	{
		if (value == null)
		{
			m_capital = null;
			return;
		}
		final PlayerID p = getData().getPlayerList().getPlayerID(value);
		if (p == null)
			throw new GameParseException("No Player named: " + value + thisErrorMsg());
		m_capital = value;
	}
	
	public boolean isCapital()
	{
		return m_capital != null;
	}
	
	public String getCapital()
	{
		return m_capital;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setVictoryCity(final String value)
	{
		m_victoryCity = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setVictoryCity(final Boolean value)
	{
		m_victoryCity = value;
	}
	
	public boolean getVictoryCity()
	{
		return m_victoryCity;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setOriginalFactory(final String value)
	{
		m_originalFactory = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setOriginalFactory(final Boolean value)
	{
		m_originalFactory = value;
	}
	
	public boolean getOriginalFactory()
	{
		return m_originalFactory;
	}
	
	/**
	 * setProduction (or just "production" in a map xml) sets both the m_production AND the m_unitProduction of a territory to be equal to the String value passed.
	 * 
	 * @param value
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setProduction(final String value)
	{
		m_production = getInt(value);
		m_unitProduction = m_production; // do NOT remove. unitProduction should always default to production
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setProduction(final Integer value)
	{
		m_production = value;
		m_unitProduction = m_production; // do NOT remove. unitProduction should always default to production
	}
	
	/**
	 * Sets only m_production
	 * 
	 * @param value
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setProductionOnly(final String value)
	{
		m_production = getInt(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setProductionOnly(final int value)
	{
		m_production = value;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setProductionOnly(final Integer value)
	{
		m_production = value;
	}
	
	public int getProduction()
	{
		return m_production;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setUnitProduction(final Integer value)
	{
		m_unitProduction = value;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setUnitProduction(final String value)
	{
		m_unitProduction = Integer.parseInt(value);
	}
	
	public int getUnitProduction()
	{
		return m_unitProduction;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setOccupiedTerrOf(final String value)
	{
		if (value == null)
		{
			m_occupiedTerrOf = null;
			return;
		}
		m_occupiedTerrOf = getData().getPlayerList().getPlayerID(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setOccupiedTerrOf(final PlayerID value)
	{
		m_occupiedTerrOf = value;
	}
	
	public PlayerID getOccupiedTerrOf()
	{
		return m_occupiedTerrOf;
	}
	
	/**
	 * Should not be set by a game xml during attachment parsing, but CAN be set by initialization parsing and/or Property Utils.
	 * 
	 * @param player
	 */
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setOriginalOwner(final PlayerID player)
	{
		m_originalOwner = player;
	}
	
	public PlayerID getOriginalOwner()
	{
		return m_originalOwner;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setConvoyRoute(final String value)
	{
		m_convoyRoute = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setConvoyRoute(final Boolean value)
	{
		m_convoyRoute = value;
	}
	
	public boolean getConvoyRoute()
	{
		return m_convoyRoute;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setChangeUnitOwners(final String value) throws GameParseException
	{
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_changeUnitOwners.add(tempPlayer);
			else if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false"))
				m_changeUnitOwners.clear();
			else
				throw new GameParseException("No player named: " + name + thisErrorMsg());
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setChangeUnitOwners(final ArrayList<PlayerID> value)
	{
		m_changeUnitOwners = value;
	}
	
	public ArrayList<PlayerID> getChangeUnitOwners()
	{
		return m_changeUnitOwners;
	}
	
	public void clearChangeUnitOwners()
	{
		m_changeUnitOwners.clear();
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
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setWhenCapturedByGoesTo(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("whenCapturedByGoesTo must have 2 player names separated by a colon" + thisErrorMsg());
		for (final String name : s)
		{
			final PlayerID player = getData().getPlayerList().getPlayerID(name);
			if (player == null)
				throw new GameParseException("No player named: " + name + thisErrorMsg());
		}
		m_whenCapturedByGoesTo.add(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWhenCapturedByGoesTo(final ArrayList<String> value)
	{
		m_whenCapturedByGoesTo = value;
	}
	
	public ArrayList<String> getWhenCapturedByGoesTo()
	{
		return m_whenCapturedByGoesTo;
	}
	
	public void clearWhenCapturedByGoesTo()
	{
		m_whenCapturedByGoesTo.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setTerritoryEffect(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		for (final String name : s)
		{
			final TerritoryEffect effect = getData().getTerritoryEffectList().get(name);
			if (effect != null)
				m_territoryEffect.add(effect);
			else
				throw new GameParseException("No TerritoryEffect named: " + name + thisErrorMsg());
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTerritoryEffect(final ArrayList<TerritoryEffect> value)
	{
		m_territoryEffect = value;
	}
	
	public ArrayList<TerritoryEffect> getTerritoryEffect()
	{
		return m_territoryEffect;
	}
	
	public void clearTerritoryEffect()
	{
		m_territoryEffect.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setConvoyAttached(final String value) throws GameParseException
	{
		if (value.length() <= 0)
			return;
		for (final String subString : value.split(":"))
		{
			final Territory territory = getData().getMap().getTerritory(subString);
			if (territory == null)
				throw new GameParseException("No territory called:" + subString + thisErrorMsg());
			m_convoyAttached.add(territory);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setConvoyAttached(final HashSet<Territory> value)
	{
		m_convoyAttached = value;
	}
	
	public HashSet<Territory> getConvoyAttached()
	{
		return m_convoyAttached;
	}
	
	public void clearConvoyAttached()
	{
		m_convoyAttached.clear();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setNavalBase(final String value)
	{
		m_navalBase = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setNavalBase(final Boolean value)
	{
		m_navalBase = value;
	}
	
	public boolean getNavalBase()
	{
		return m_navalBase;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirBase(final String value)
	{
		m_airBase = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAirBase(final Boolean value)
	{
		m_airBase = value;
	}
	
	public boolean getAirBase()
	{
		return m_airBase;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setKamikazeZone(final String value)
	{
		m_kamikazeZone = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setKamikazeZone(final Boolean value)
	{
		m_kamikazeZone = value;
	}
	
	public boolean getKamikazeZone()
	{
		return m_kamikazeZone;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBlockadeZone(final String value)
	{
		m_blockadeZone = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setBlockadeZone(final Boolean value)
	{
		m_blockadeZone = value;
	}
	
	public boolean getBlockadeZone()
	{
		return m_blockadeZone;
	}
	
	public static Set<Territory> getWhatTerritoriesThisIsUsedInConvoysFor(final Territory t, final GameData data)
	{
		final Set<Territory> rVal = new HashSet<Territory>();
		final TerritoryAttachment ta = TerritoryAttachment.get(t);
		if (ta == null || !ta.getConvoyRoute())
			return null;
		for (final Territory current : data.getMap().getTerritories())
		{
			final TerritoryAttachment cta = TerritoryAttachment.get(current);
			if (cta == null || !cta.getConvoyRoute())
				continue;
			if (cta.getConvoyAttached().contains(t))
				rVal.add(current);
		}
		return rVal;
	}
	
	public String toStringForInfo(final boolean useHTML, final boolean includeAttachedToName)
	{
		final StringBuilder sb = new StringBuilder("");
		final String br = (useHTML ? "<br>" : ", ");
		final Territory t = (Territory) this.getAttachedTo();
		if (t == null)
			return sb.toString();
		if (includeAttachedToName)
		{
			sb.append(t.getName());
			sb.append(br);
			if (t.isWater())
				sb.append("Water Territory");
			else
				sb.append("Land Territory");
			sb.append(br);
			final PlayerID owner = t.getOwner();
			if (owner != null && !owner.isNull())
			{
				sb.append("Owned by " + t.getOwner().getName());
				sb.append(br);
			}
		}
		
		if (m_isImpassible)
		{
			sb.append("Is Impassable");
			sb.append(br);
		}
		
		if (m_capital != null && m_capital.length() > 0)
		{
			sb.append("A Capital of " + m_capital);
			sb.append(br);
		}
		
		if (m_victoryCity)
		{
			sb.append("Is a Victory location");
			sb.append(br);
		}
		
		if (m_kamikazeZone)
		{
			sb.append("Is Kamikaze Zone");
			sb.append(br);
		}
		
		if (m_blockadeZone)
		{
			sb.append("Is a Blockade Zone");
			sb.append(br);
		}
		
		if (m_convoyRoute)
		{
			if (!m_convoyAttached.isEmpty())
				sb.append("Needs: " + MyFormatter.asList(m_convoyAttached) + br);
			final Set<Territory> requiredBy = getWhatTerritoriesThisIsUsedInConvoysFor(t, getData());
			if (!requiredBy.isEmpty())
				sb.append("Required By: " + MyFormatter.asList(requiredBy) + br);
		}
		
		if (!t.isWater() && m_production > 0 && games.strategy.triplea.Properties.getSBRAffectsUnitProduction(getData()))
		{
			sb.append("Unit Production: ");
			sb.append(m_unitProduction);
			sb.append(br);
		}
		else if (!t.isWater() && m_unitProduction > 0 && games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(getData()))
		{
			sb.append("Base Unit Production: ");
			sb.append(m_unitProduction);
			sb.append(br);
		}
		
		if (m_production > 0 || (m_resources != null && m_resources.toString().length() > 0))
		{
			sb.append("Production: ");
			sb.append(br);
			if (m_production > 0)
			{
				sb.append("&nbsp;&nbsp;&nbsp;&nbsp;" + m_production + " PUs");
				sb.append(br);
			}
			if (m_resources != null)
			{
				if (useHTML)
					sb.append("&nbsp;&nbsp;&nbsp;&nbsp;" + (m_resources.toStringForHTML()).replaceAll("<br>", "<br>&nbsp;&nbsp;&nbsp;&nbsp;"));
				else
					sb.append(m_resources.toString());
				sb.append(br);
			}
		}
		
		final Iterator<TerritoryEffect> iter = m_territoryEffect.iterator();
		if (iter.hasNext())
		{
			sb.append("Territory Effects: ");
			sb.append(br);
		}
		while (iter.hasNext())
		{
			sb.append("&nbsp;&nbsp;&nbsp;&nbsp;" + iter.next().getName());
			sb.append(br);
		}
		
		return sb.toString();
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
