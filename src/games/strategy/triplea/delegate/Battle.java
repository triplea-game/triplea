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
 * Battle.java
 *
 * Created on November 15, 2001, 12:39 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.DelegateBridge;

import games.strategy.triplea.Constants;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Represents a battle.
 */
interface Battle extends java.io.Serializable
{
    /**
     *  Add a bunch of attacking units to the battle.
     */
    public void addAttack(Route route, Collection units);
    

    /**
     * Return whether this battle is a bombing run.
     */
    public boolean isBombingRun();
    
    /**
     * Return what territory this battle is occuring in.
     */
    public Territory getTerritory();
    
    /**
     * Fight this battle.
     */
    public void fight(DelegateBridge bridge);
    
    /**
     * Return whether this battle is over or not.
     */
    public boolean isOver();
    
    /**
     * Call this method when units are lost in another battle.
     * This is needed to remove dependent units who have been
     * lost in another battle.
     */
    public void unitsLost(Battle battle, Collection units, DelegateBridge bridge);

    /**
     * Add a bombardment unit.
     */
    public void addBombardingUnit(Unit u);

    /**
     * Return whether battle is amphibious
     */
    public boolean isAmphibious();

    /**
     *  This occurs when a move has been undone
     */
    public void removeAttack(Route route, Collection units);

    /**
     * After an attack has been removed, you can use this to test if
    * there are still units left to fight
     */
    public boolean isEmpty();

    /**
     * Return units which are dependent on the given units.
     */
    public Collection getDependentUnits(Collection units);
}
