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
 * Unit.java
 * 
 * Created on October 14, 2001, 12:33 PM
 */
package games.strategy.engine.data;

import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.net.GUID;

import java.io.Serializable;

/**
 * 
 * @author Sean Bridges
 */
public class Unit extends GameDataComponent implements Serializable
{
	private static final long serialVersionUID = -7906193079642776282L;
	private PlayerID m_owner;
	private final GUID m_uid;
	private int m_hits = 0;
	private final UnitType m_type;
	
	/**
	 * Creates new Unit. Should use a call to UnitType.create() instead.
	 * owner can be null
	 */
	protected Unit(final UnitType type, final PlayerID owner, final GameData data)
	{
		super(data);
		if (type == null)
		{
			throw new IllegalArgumentException();
		}
		m_type = type;
		m_uid = new GUID();
		setOwner(owner);
	}
	
	public GUID getID()
	{
		return m_uid;
	}
	
	public UnitType getType()
	{
		return m_type;
	}
	
	public UnitType getUnitType()
	{
		return m_type;
	}
	
	public PlayerID getOwner()
	{
		return m_owner;
	}
	
	/**
	 * DO NOT USE THIS METHOD if at all possible. It is very slow.
	 * This can return null if the unit is not in any territories.
	 * A unit just created, or held by a player after purchasing may not be in a territory.
	 * A unit can be in exactly 2 territories, if the unit is in the process of moving from one territory to another. This method will just return the first territory found.
	 * A unit should never be in more than 2 territories.
	 */
	@Deprecated
	public Territory getTerritoryUnitIsIn()
	{
		// Collection<Territory> terrs = new ArrayList<Territory>();
		for (final Territory t : this.getData().getMap().getTerritories())
		{
			if (t.getUnits().getUnits().contains(this))
				return t;
			// terrs.add(t);
		}
		return null;
		/*if (terrs.size() > 2)
			throw new IllegalStateException("Unit, " + this.toString() + ", may not be in multiple territories at the same time.");
		else if (terrs.size() == 2)
			return terrs.iterator().next(); // this actually does occur while in the middle of moving a unit from one territory to another, before the unit gets deleted from the first.
		else if (terrs.size() == 1)
			return terrs.iterator().next();
		else
			return null;*/
	}
	
	public int getHits()
	{
		return m_hits;
	}
	
	/**
	 * Remember to always use a ChangeFactory change over an IDelegate Bridge for any changes to game data, or any change that should go over the network.
	 * 
	 * @param hits
	 */
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setHits(final int hits)
	{
		m_hits = hits;
	}
	
	/**
	 * can be null.
	 */
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	void setOwner(PlayerID player)
	{
		if (player == null)
			player = PlayerID.NULL_PLAYERID;
		m_owner = player;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if (!(o instanceof Unit))
			return false;
		final Unit other = (Unit) o;
		return this.m_uid.equals(other.m_uid);
	}
	
	@Override
	public int hashCode()
	{
		return m_uid.hashCode();
	}
	
	@Override
	public String toString()
	{
		// none of these should happen,... except that they did a couple times.
		if (m_type == null || m_owner == null)
		{
			final String text = "Unit.toString() -> Possible java de-serialization error: " + (m_type == null ? "Unit of UNKNOWN TYPE" : m_type.getName())
						+ " owned by " + (m_owner == null ? "UNKNOWN OWNER" : m_owner.getName()) + " in territory: "
						+ ((this.getData() != null && this.getData().getMap() != null) ? getTerritoryUnitIsIn() : "UNKNOWN TERRITORY") + " with id: " + getID();
			System.err.println(text);
			return text;
		}
		return m_type.getName() + " owned by " + m_owner.getName();
	}
	
	public String toStringNoOwner()
	{
		return m_type.getName();
	}
}
