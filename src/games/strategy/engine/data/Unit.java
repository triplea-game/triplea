/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * Unit.java
 *
 * Created on October 14, 2001, 12:33 PM
 */

package games.strategy.engine.data;

import games.strategy.net.GUID;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;


/**
 *
 * @author  Sean Bridges
 */
public class Unit extends GameDataComponent implements Serializable
{
  private PlayerID m_owner;
  private final GUID m_uid;
  private int m_hits;
  private final UnitType m_type;  

  /**
   * Creates new Unit.  Should use a call to UnitType.create() instead.
   * owner can be null
   */
  protected Unit(UnitType type, PlayerID owner, GameData data)
  {
    super(data);
    if(type == null) {
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
   * This can return null if the unit is not in any territories.  
   * A unit just created, or held by a player after purchasing may not be in a territory.
   * A unit can be in exactly 2 territories, if the unit is in the process of moving from one territory to another. This method will just return the first territory found.
   * A unit should never be in more than 2 territories, and if so an error will be thrown.  
   */
  public Territory getTerritoryUnitIsIn()
  {
	Collection<Territory> terrs = new ArrayList<Territory>();
    for (Territory t : this.getData().getMap().getTerritories())
    {
    	if (t.getUnits().getUnits().contains(this))
    		terrs.add(t);
    }
    if (terrs.size() > 2)
    	throw new IllegalStateException("Unit, " + this.toString() + ", may not be in multiple territories at the same time.");
    else if (terrs.size() == 2)
    	return terrs.iterator().next(); // this actually does occur while in the middle of moving a unit from one territory to another, before the unit gets deleted from the first.
    else if (terrs.size() == 1)
    	return terrs.iterator().next();
    else
    	return null;
  }

  public int getHits()
  {
      return m_hits;
  }

  void setHits(int hits)
  {
      m_hits = hits;
  }

  /**
   * can be null.
   */
  void setOwner(PlayerID player)
  {
    if(player == null)
      player = PlayerID.NULL_PLAYERID;
    m_owner = player;
  }

  public boolean equals(Object o)
  {
    if(! (o instanceof Unit))
      return false;
    
    Unit other = (Unit) o;
    return this.m_uid.equals(other.m_uid);
  }

  public int hashCode()
  {
    return m_uid.hashCode();
  }

  public String toString()
  {
    return m_type.getName() + " owned by " + m_owner.getName();
  }



}
