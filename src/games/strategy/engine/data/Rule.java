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
public class Rule extends NamedAttachable implements NamedUnitHolder, Serializable, Comparable<Rule>
{
  private static final long serialVersionUID = -6390555051736721082L;

  private final boolean m_water;
  private PlayerID m_owner = PlayerID.NULL_PLAYERID;
  private final UnitCollection m_units;

  // In a grid-based game, stores the coordinate of the Territory
  int[] m_coordinate = null;
  
  /** Creates new Territory */
  public Rule(String name, boolean water, GameData data)
  {
    super(name, data);
    m_water = water;
    m_units = new UnitCollection(this, getData());
  }

  /** Creates new Territory */
  public Rule(String name, boolean water, GameData data, int... coordinate)
  {
    super(name, data);
    m_water = water;
    m_units = new UnitCollection(this, getData());
    
    if (data.getMap().isCoordinateValid(coordinate))
        m_coordinate = coordinate;
    else
        throw new IllegalArgumentException("Invalid coordinate: " + coordinate[0] + "," + coordinate[1]);
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

  /**
   * Get the units in this territory
   */
  public UnitCollection getUnits()
  {
    return m_units;
  }

  public void notifyChanged()
  {
    
  }

  public String toString()
  {
    return getName();
  }

  public int compareTo(Rule r)
  {
    return getName().compareTo(r.getName());
  }

  public String getType()
  {
    return UnitHolder.TERRITORY;
  }
  
  public boolean matchesCoordinates(int... coordinate) 
  {
      if (coordinate.length != m_coordinate.length)
          return false;
      else
      {
          for (int i=0; i<coordinate.length; i++)
          {
              if (coordinate[i] != m_coordinate[i])
                  return false;
          }          
      }
      
      return true;
  }
  
  public int getX() 
  {
      try {
          return m_coordinate[0];
      }
      catch (ArrayIndexOutOfBoundsException e)
      {
          throw new RuntimeException("Territory " + this.getName() + " doesn't have a defined x coordinate");
      }
  }

  public int getY() 
  {
      try {
          return m_coordinate[1];
      }
      catch (ArrayIndexOutOfBoundsException e)
      {
          throw new RuntimeException("Territory " + this.getName() + " doesn't have a defined y coordinate");
      }
  }
  
}
