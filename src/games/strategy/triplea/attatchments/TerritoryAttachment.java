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
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.triplea.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TerritoryAttachment extends DefaultAttachment
{
	
	public static Territory getCapital(PlayerID player, GameData data)
	{
		Iterator<Territory> iter = data.getMap().getTerritories().iterator();
		while (iter.hasNext())
		{
			Territory current = iter.next();
			TerritoryAttachment ta = TerritoryAttachment.get(current);
			if (ta != null && ta.getCapital() != null)
			{
				PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
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
	public static List<Territory> getAllCapitals(PlayerID player, GameData data)
	{
		List<Territory> capitals = new ArrayList<Territory>();
		Iterator<Territory> iter = data.getMap().getTerritories().iterator();
		while (iter.hasNext())
		{
			Territory current = iter.next();
			TerritoryAttachment ta = TerritoryAttachment.get(current);
			if (ta != null && ta.getCapital() != null)
			{
				PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
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
	public static List<Territory> getAllCurrentlyOwnedCapitals(PlayerID player, GameData data)
	{
		List<Territory> capitals = new ArrayList<Territory>();
		Iterator<Territory> iter = data.getMap().getTerritories().iterator();
		while (iter.hasNext())
		{
			Territory current = iter.next();
			TerritoryAttachment ta = TerritoryAttachment.get(current);
			if (ta != null && ta.getCapital() != null)
			{
				PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
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
	public static TerritoryAttachment get(Territory t)
	{
		TerritoryAttachment rVal = (TerritoryAttachment) t.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
		if (rVal == null && !t.isWater())
			throw new IllegalStateException("No territory attachment for:" + t.getName());
		return rVal;
	}
	
	public static TerritoryAttachment get(Territory t, String nameOfAttachment)
	{
		TerritoryAttachment rVal = (TerritoryAttachment) t.getAttachment(nameOfAttachment);
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
	private Collection<PlayerID> m_changeUnitOwners = new ArrayList<PlayerID>();
	private Collection<PlayerID> m_captureUnitOnEnteringBy = new ArrayList<PlayerID>();
	private String m_convoyAttached = null;
	private boolean m_navalBase = false;
	private boolean m_airBase = false;
	private boolean m_kamikazeZone = false;
	private int m_unitProduction = 0;
	private boolean m_blockadeZone = false;
	private Collection<TerritoryEffect> m_territoryEffect = new ArrayList<TerritoryEffect>();
	private Collection<String> m_whenCapturedByGoesTo = new ArrayList<String>();
	
	/** Creates new TerritoryAttatchment */
	public TerritoryAttachment()
	{
	}
	
	public void setIsImpassible(String value)
	{
		m_isImpassible = getBool(value);
	}
	
	public boolean isImpassible()
	{
		return m_isImpassible;
	}
	
	public void setCapital(String value)
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
	
	public void setVictoryCity(String value)
	{
		m_isVictoryCity = getBool(value);
	}
	
	public boolean isVictoryCity()
	{
		return m_isVictoryCity;
	}
	
	public void setOriginalFactory(String value)
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
	public void setProduction(String value)
	{
		m_production = getInt(value);
		m_unitProduction = m_production; // do NOT remove. unitProduction should always default to production
	}
	
	/**
	 * Sets only m_production
	 * 
	 * @param value
	 */
	public void setProductionOnly(int value)
	{
		m_production = value;
	}
	
	/**
	 * Sets only m_production
	 * 
	 * @param value
	 */
	public void setProductionOnly(String value)
	{
		m_production = getInt(value);
	}
	
	public int getProduction()
	{
		return m_production;
	}
	
	public void setUnitProduction(Integer value)
	{
		m_unitProduction = value;
	}
	
	public void setUnitProduction(String value)
	{
		m_unitProduction = Integer.parseInt(value);
	}
	
	public int getUnitProduction()
	{
		return m_unitProduction;
	}
	
	public void setOccupiedTerrOf(String value)
	{
		if (value != null)
			m_occupiedTerrOf = getData().getPlayerList().getPlayerID(value);
	}
	
	public PlayerID getOccupiedTerrOf()
	{
		return m_occupiedTerrOf;
	}
	
	public void setOriginalOwner(PlayerID player)
	{
		m_originalOwner = player;
	}
	
	public PlayerID getOriginalOwner()
	{
		return m_originalOwner;
	}
	
	public void setConvoyRoute(String value)
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
	public void setChangeUnitOwners(String value)
	{
		String[] temp = value.split(":");
		for (String name : temp)
		{
			PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
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
	public void setCaptureUnitOnEnteringBy(String value)
	{
		String[] temp = value.split(":");
		for (String name : temp)
		{
			PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
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
	public void setWhenCapturedByGoesTo(String value) throws GameParseException
	{
		String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("whenCapturedByGoesTo must have 2 player names separated by a colon");
		for (String name : s)
		{
			PlayerID player = getData().getPlayerList().getPlayerID(name);
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
	public void setTerritoryEffect(String value) throws GameParseException
	{
		String[] s = value.split(":");
		for (String name : s)
		{
			TerritoryEffect effect = getData().getTerritoryEffectList().get(name);
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
	
	public void setConvoyAttached(String value)
	{
		m_convoyAttached = value;
	}
	
	public String getConvoyAttached()
	{
		return m_convoyAttached;
	}
	
	public void setNavalBase(String value)
	{
		m_navalBase = getBool(value);
	}
	
	public boolean isNavalBase()
	{
		return m_navalBase;
	}
	
	public void setAirBase(String value)
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
	
	public void setKamikazeZone(String value)
	{
		m_kamikazeZone = getBool(value);
	}
	
	public void setBlockadeZone(String value)
	{
		m_blockadeZone = getBool(value);
	}
	
	public boolean isBlockadeZone()
	{
		return m_blockadeZone;
	}
}
