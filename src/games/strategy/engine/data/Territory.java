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
 * Territory.java
 *
 * Created on October 12, 2001, 1:50 PM
 */

package games.strategy.engine.data;

import java.io.Serializable;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class Territory extends NamedAttachable implements NamedUnitHolder, Serializable, Comparable<Territory>
{
  private static final long serialVersionUID = -6390555051736721082L;

  private final boolean m_water;
  private PlayerID m_owner = PlayerID.NULL_PLAYERID;
  private final UnitCollection m_units;

  /** Creates new Territory */
  public Territory(String name, boolean water, GameData data)
  {
    super(name, data);
    m_water = water;
    m_units = new UnitCollection(this, getData());
  }

  public boolean isWater()
  {
    return m_water;
  }

  /**
   * May be null if not owned.
   */
  public PlayerID getOwner()
  {
    return m_owner;
  }

  public void setOwner(PlayerID newOwner)
  {
    if (newOwner == null)
      newOwner = PlayerID.NULL_PLAYERID;
    m_owner = newOwner;
    getData().notifyTerritoryOwnerChanged(this);
  }

  /**
   * Get the units in this territory
   */
  public UnitCollection getUnits()
  {
    return m_units;
  }

  public void notifyChanged()
  {
    getData().notifyTerritoryUnitsChanged(this);
  }

  public String toString()
  {
    return getName();
  }

  public int compareTo(Territory o)
  {
    return getName().compareTo(o.getName());
  }

  public String getType()
  {
    return UnitHolder.TERRITORY;
  }
}
