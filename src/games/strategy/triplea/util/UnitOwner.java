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



package games.strategy.triplea.util;


import games.strategy.engine.data.*;

public class UnitOwner
{
  private UnitType m_type;
  private PlayerID m_owner;

  public UnitOwner(Unit unit)
  {
    m_type = unit.getType();
    m_owner = unit.getOwner();
  }

  public UnitOwner(UnitType type, PlayerID owner)
  {
    m_type = type;
    m_owner = owner;
  }

  public boolean equals(Object o)
  {

    if(o == null)
      return false;
    if(!(o instanceof UnitOwner))
       return false;

    UnitOwner other = (UnitOwner) o;
    return other.m_type.equals(this.m_type) &&
        other.m_owner.equals(this.m_owner);
  }

  public int hashCode()
  {
    return m_type.hashCode() ^ m_owner.hashCode();
  }


  public String toString()
  {
    return "Unit owner:" + m_owner.getName() + " type:" + m_type.getName();
  }

  public UnitType getType()
  {
    return m_type;
  }

  public PlayerID getOwner()
  {
    return m_owner;
  }
}
