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

package games.strategy.triplea.player;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.data.PlayerID;
import games.strategy.net.IRemote;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.message.CasualtyDetails;

/**
 * Interface the TriplePlayer presents to Delegates through IRemoteMessenger
 * 
 * @author Sean Bridges
 */
public interface ITripleaPlayer extends IRemote
{

    /**
     * Select casualties
     * 
     * @param m_selectFrom - the units to select casualties from
     * @param m_dependents - dependents of the units to select from
     * @param m_count - the number of casualties to select
     * @param m_message - ui message to display
     * @param m_hit - the player hit
     * @param m_dice - the dice rolled for the casualties 
     * @param m_defaultCasualties - default casualties as selected by the game
     * @return the selected casualties
     */
    public CasualtyDetails selectCasualties(
            String step, Collection selectFrom, Map dependents,  int count, String message, DiceRoll dice, PlayerID hit, List defaultCasualties     
    );
    
    /**
     * Select the territory to bombard
     * 
     * @param unit - the bombarding unit
     * @param unitTerritory - where the bombarding unit is
     * @param territories - territories where the unit can bombard
     * @param noneAvailable 
     * @return the Territory to bombard in, null if the unit should not bombard
     */
    public Territory selectBombardingTerritory(
            Unit unit,
            Territory unitTerritory,
            Collection territories,
            boolean noneAvailable       
    );
    
    
    /**
     * 
     * 
     * @param report that an error occured
     */
    public void reportError(String error);
    

    /**
     * One or more bombers have just moved into a territory where a strategic bombing
     * raid can be conducted, should the bomber bomb? 
     */
    public boolean shouldBomberBomb(Territory territory);
    
    /**
     * Choose where my rockets should fire
     * 
     * @param candidates  - a collection of Territories,  the possible territories to attack
     * @param from - where the rockets are launched from, null for 3rd edition rules
     * @return the territory to attack, null if no territory should be attacked
     */
    public Territory whereShouldRocketsAttach(Collection candidates, Territory from);
    
    /**
     * get the number of fighters to move to a newly produced carrier
     * 
     * @param fightersThatCanBeMoved - the fighters that can be moved
     * @param from - the territory containing the factory
     * @return - the fighters to move
     */
    public Collection getNumberOfFightersToMoveToNewCarrier(Collection fightersThatCanBeMoved, Territory from);
 
    /**
     * Some carriers were lost while defending.  We must select where to land
     * some air units.
     * 
     * @param candidates -  a list of territories - these are the places where air units can land
     * @return - the territory to land the fighters in, must be non null
     */
    public Territory selectTerritoryForAirToLand(Collection candidates);
    
    /**
     * Notifies that the units have retreated or submerged, and no longer take part in the battle. 
     */
    public void retreatNotificationMessage(Collection units);
    
}
