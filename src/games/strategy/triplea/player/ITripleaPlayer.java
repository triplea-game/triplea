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
import games.strategy.engine.message.IRemote;
import games.strategy.net.*;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;

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
     * @param selectFrom - the units to select casualties from
     * @param dependents - dependents of the units to select from
     * @param count - the number of casualties to select
     * @param message - ui message to display
     * @param hit - the player hit
     * @param dice - the dice rolled for the casualties 
     * @param defaultCasualties - default casualties as selected by the game
     * @param the battle step the selection is for
     * @return the selected casualties
     */
    public CasualtyDetails selectCasualties(
            String step, Collection selectFrom, Map dependents,  int count, String message, DiceRoll dice, PlayerID hit, List defaultCasualties     
    );
    
    /**
     * Select the territory to bombard with the bombarding capable unit (eg battleship)
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
     * Report an error to the user. 
     * 
     * @param report that an error occured
     */
    public void reportError(String error);
    
    /**
     * report a message to the user
     * @param message
     */
    public void reportMessage(String message);
    
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
     * get the fighters to move to a newly produced carrier
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
     * The attempted move will incur aa fire, confirm that you still want to move
     * 
     * @param  aaFiringTerritories - the territories where aa will fire
     */
    public boolean confirmMoveInFaceOfAA(Collection aaFiringTerritories);
    
    /**
     * 
     * Ask the player if he wishes to retreat.
     * 
     * @param battleID - the battle
     * @param submerge - is submerging possible
     * @param possibleTerritories - where the player can retreat to
     * @param message - user displayable message
     * @param step - the battle step
     * @return the territory to retreat to, or null if the player doesnt wish to retreat
     */
    public Territory retreatQuery(GUID battleID, boolean submerge, Collection possibleTerritories, String message, String step);

    
    /**
     * Allows the user to pause and confirm enemy casualties
     * 
     * @param battleId
     * @param message
     * @param step
     */
    public void confirmEnemyCasualties(GUID battleId, String message, String step, PlayerID hitPlayer);
    
}