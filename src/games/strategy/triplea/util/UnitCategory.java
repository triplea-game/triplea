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


import java.util.*;
import games.strategy.util.*;
import games.strategy.engine.data.*;

public class UnitCategory implements Comparable
{
   private UnitType m_type;
   //Collection of UnitOwners, the type of our dependents, not the dependents
   private Collection m_dependents;
   private int m_movement; //movement of the units
   private PlayerID m_owner;
   //the units in the category
   private List m_units = new ArrayList();

   public UnitCategory(Unit unit, Collection dependents, int movement)
   {
     m_type = unit.getType();
     m_movement = movement;
     m_owner = unit.getOwner();
     if(dependents == null)
       m_dependents = new ArrayList();
     else
       m_dependents = new ArrayList(dependents);
     m_units.add(unit);
     createDependents(dependents);
   }

   private void createDependents(Collection dependents)
   {
     m_dependents = new ArrayList();

     if(dependents == null)
       return;

     Iterator iter = dependents.iterator();

     while(iter.hasNext())
     {
       Unit current = (Unit) iter.next();
       m_dependents.add(new UnitOwner(current));
     }
   }



   public boolean equals(Object o)
   {
     if(o == null)
       return false;
     if(!(o instanceof UnitCategory))
        return false;

     UnitCategory other = (UnitCategory) o;

     //equality of categories does not compare the number
     //of units in the category, so dont compare on m_units
     return
       other.m_type.equals(this.m_type) &&
       other.m_movement == this.m_movement &&
       other.m_owner.equals(this.m_owner) &&
       Util.equals(this.m_dependents, other.m_dependents);
   }

   public int hashCode()
   {
     return m_type.hashCode();
   }

   public String toString()
   {
     return "Entry type:" + m_type.getName() + " dependenents:" + m_dependents;
   }

   /**
    * Collection of UnitOwners, the type of our dependents, not the dependents
    */
   public Collection getDependents()
   {
     return m_dependents;
   }

   public List getUnits()
   {
     return m_units;
   }

   public int getMovement()
   {
     return m_movement;
   }

   public PlayerID getOwner()
   {
     return m_owner;
   }

   public void addUnit(Unit unit)
   {
     m_units.add(unit);
   }

   public UnitType getType()
   {
     return m_type;
   }

   public int compareTo(Object o)
   {
     if(o == null)
       return -1;

     UnitCategory other = (UnitCategory) o;
     if(!other.m_owner.equals(this.m_owner))
       return other.m_owner.getName().compareTo(this.m_owner.getName());

     return other.getType().getName().compareTo(this.getType().getName());
   }
}

