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

import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.triplea.Constants;
import games.strategy.triplea.formatter.MyFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TerritoryAttachment extends DefaultAttachment
{
	public static Territory getCapital(final PlayerID player, final GameData data)
	{
		final Iterator<Territory> iter = data.getMap().getTerritories().iterator();
		while (iter.hasNext())
		{
			final Territory current = iter.next();
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
		final Iterator<Territory> iter = data.getMap().getTerritories().iterator();
		while (iter.hasNext())
		{
			final Territory current = iter.next();
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
		final Iterator<Territory> iter = data.getMap().getTerritories().iterator();
		while (iter.hasNext())
		{
			final Territory current = iter.next();
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
	private int m_production = 0;
	private boolean m_isVictoryCity = false;
	private boolean m_isImpassible = false;
	private PlayerID m_originalOwner = null;
	private PlayerID m_occupiedTerrOf = null;
	private boolean m_isConvoyRoute = false;
	private final Set<Territory> m_convoyAttached = new HashSet<Territory>();
	private final Collection<PlayerID> m_changeUnitOwners = new ArrayList<PlayerID>();
	private final Collection<PlayerID> m_captureUnitOnEnteringBy = new ArrayList<PlayerID>();
	private boolean m_navalBase = false;
	private boolean m_airBase = false;
	private boolean m_kamikazeZone = false;
	private int m_unitProduction = 0;
	private boolean m_blockadeZone = false;
	private final Collection<TerritoryEffect> m_territoryEffect = new ArrayList<TerritoryEffect>();
	private final Collection<String> m_whenCapturedByGoesTo = new ArrayList<String>();
	private ResourceCollection m_resources = null;
	
	/** Creates new TerritoryAttatchment */
	public TerritoryAttachment()
	{
	}
	
	public void setResources(final String value) throws GameParseException
	{
		if (m_resources == null)
			m_resources = new ResourceCollection(getData());
		final String[] s = value.split(":");
		final int amount = getInt(s[0]);
		if (s[1].equals(Constants.PUS))
			throw new GameParseException("Territory Attachments: Please set PUs using production, not resource");
		final Resource resource = getData().getResourceList().getResource(s[1]);
		if (resource == null)
			throw new GameParseException("Territory Attachments: No resource named: " + s[1]);
		m_resources.putResource(resource, amount);
	}
	
	public ResourceCollection getResources()
	{
		return m_resources;
	}
	
	public void clearResources()
	{
		m_resources = new ResourceCollection(getData());
	}
	
	public void setIsImpassible(final String value)
	{
		m_isImpassible = getBool(value);
	}
	
	public boolean isImpassible()
	{
		return m_isImpassible;
	}
	
	public void setCapital(final String value)
	{
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
	
	public void setVictoryCity(final String value)
	{
		m_isVictoryCity = getBool(value);
	}
	
	public boolean isVictoryCity()
	{
		return m_isVictoryCity;
	}
	
	public void setOriginalFactory(final String value)
	{
		m_originalFactory = getBool(value);
	}
	
	public boolean isOriginalFactory()
	{
		return m_originalFactory;
	}
	
	/**
	 * setProduction (or just "production" in a map xml) sets both the m_production AND the m_unitProduction of a territory to be equal to the String value passed.
	 * 
	 * @param value
	 */
	public void setProduction(final String value)
	{
		m_production = getInt(value);
		m_unitProduction = m_production; // do NOT remove. unitProduction should always default to production
	}
	
	/**
	 * Sets only m_production
	 * 
	 * @param value
	 */
	public void setProductionOnly(final int value)
	{
		m_production = value;
	}
	
	/**
	 * Sets only m_production
	 * 
	 * @param value
	 */
	public void setProductionOnly(final String value)
	{
		m_production = getInt(value);
	}
	
	public int getProduction()
	{
		return m_production;
	}
	
	public void setUnitProduction(final Integer value)
	{
		m_unitProduction = value;
	}
	
	public void setUnitProduction(final String value)
	{
		m_unitProduction = Integer.parseInt(value);
	}
	
	public int getUnitProduction()
	{
		return m_unitProduction;
	}
	
	public void setOccupiedTerrOf(final String value)
	{
		if (value != null)
			m_occupiedTerrOf = getData().getPlayerList().getPlayerID(value);
	}
	
	public PlayerID getOccupiedTerrOf()
	{
		return m_occupiedTerrOf;
	}
	
	public void setOriginalOwner(final PlayerID player)
	{
		m_originalOwner = player;
	}
	
	public PlayerID getOriginalOwner()
	{
		return m_originalOwner;
	}
	
	public void setConvoyRoute(final String value)
	{
		m_isConvoyRoute = getBool(value);
	}
	
	public boolean isConvoyRoute()
	{
		return m_isConvoyRoute;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	public void setChangeUnitOwners(final String value)
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
				throw new IllegalStateException("Territory Attachments: No player named: " + name);
		}
	}
	
	public Collection<PlayerID> getChangeUnitOwners()
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
	 */
	public void setCaptureUnitOnEnteringBy(final String value)
	{
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_captureUnitOnEnteringBy.add(tempPlayer);
			else
				throw new IllegalStateException("Territory Attachments: No player named: " + name);
		}
	}
	
	public Collection<PlayerID> getCaptureUnitOnEnteringBy()
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
	public void setWhenCapturedByGoesTo(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("whenCapturedByGoesTo must have 2 player names separated by a colon");
		for (final String name : s)
		{
			final PlayerID player = getData().getPlayerList().getPlayerID(name);
			if (player == null)
				throw new IllegalStateException("Territory Attachments: No player named: " + name);
		}
		m_whenCapturedByGoesTo.add(value);
	}
	
	public Collection<String> getWhenCapturedByGoesTo()
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
	public void setTerritoryEffect(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		for (final String name : s)
		{
			final TerritoryEffect effect = getData().getTerritoryEffectList().get(name);
			if (effect != null)
				m_territoryEffect.add(effect);
			else
				throw new GameParseException("Territory Attachments: No TerritoryEffect named: " + name);
		}
	}
	
	public Collection<TerritoryEffect> getTerritoryEffect()
	{
		return m_territoryEffect;
	}
	
	public void clearTerritoryEffect()
	{
		m_territoryEffect.clear();
	}
	
	public void setConvoyAttached(final String value)
	{
		if (value.length() <= 0)
			return;
		for (final String subString : value.split(":"))
		{
			final Territory territory = getData().getMap().getTerritory(subString);
			if (territory == null)
				throw new IllegalStateException("Territory Attachment: No territory called:" + subString);
			m_convoyAttached.add(territory);
		}
	}
	
	public Set<Territory> getConvoyAttached()
	{
		return m_convoyAttached;
	}
	
	public void clearConvoyAttached()
	{
		m_convoyAttached.clear();
	}
	
	public void setNavalBase(final String value)
	{
		m_navalBase = getBool(value);
	}
	
	public boolean isNavalBase()
	{
		return m_navalBase;
	}
	
	public void setAirBase(final String value)
	{
		m_airBase = getBool(value);
	}
	
	public boolean isAirBase()
	{
		return m_airBase;
	}
	
	public boolean isKamikazeZone()
	{
		return m_kamikazeZone;
	}
	
	public void setKamikazeZone(final String value)
	{
		m_kamikazeZone = getBool(value);
	}
	
	public void setBlockadeZone(final String value)
	{
		m_blockadeZone = getBool(value);
	}
	
	public boolean isBlockadeZone()
	{
		return m_blockadeZone;
	}
	
	public static Set<Territory> getWhatTerritoriesThisIsUsedInConvoysFor(final Territory t, final GameData data)
	{
		final Set<Territory> rVal = new HashSet<Territory>();
		final TerritoryAttachment ta = TerritoryAttachment.get(t);
		if (ta == null || !ta.isConvoyRoute())
			return null;
		for (final Territory current : data.getMap().getTerritories())
		{
			final TerritoryAttachment cta = TerritoryAttachment.get(current);
			if (cta == null || !cta.isConvoyRoute())
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
		final Territory t = (Territory) this.getAttatchedTo();
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
		
		if (m_isVictoryCity)
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
		
		if (m_isConvoyRoute)
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
}
