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
package games.strategy.triplea.ui.display;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplay;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;

/**
 * 
 *
 *
 * @author Sean Bridges
 */
public interface ITripleaDisplay extends IDisplay
{
    /**
     * Display info about the battle.
     * This is the first message to be displayed in a battle
     * 
     * @param battleID - a unique id for the battle
     * @param location - where the battle occurs
     * @param battleTitle - the title of the battle
     * @param attackingUnits - attacking units
     * @param defendingUnits - defending units
     * @param dependentUnits - unit dependencies, maps Unit->Collection of units
     */
    public void showBattle(GUID battleID, Territory location, String battleTitle, Collection<Unit> attackingUnits, Collection<Unit> defendingUnits, Map<Unit, Collection<Unit>> dependentUnits, PlayerID attacker, PlayerID defender);
    
    /**
     * 
     * @param battleID - the battle we are listing steps for
     * @param currentStep - the current step
     * @param steps - a collection of strings denoting all steps in the battle 
     */
    public void listBattleSteps(GUID battleID, List steps);
    
    /**
     * The given battle has eneded. 
     */
    public void battleEnd(GUID battleID, String message);
    
    
    
    /**
     * Notify that the casuatlies occured
     *  
     */
    public void casualtyNotification(GUID battleID,
            String step,
            DiceRoll dice,
            PlayerID player,
            Collection<Unit> killed,
            Collection<Unit> damaged,
            Map<Unit,Collection<Unit>> dependents);
    
    /**
     * Notification of the results of a bombing raid 
     */
    public void bombingResults(GUID battleID, int[] dice, int cost);
    
    /**
     * Notify that the given player has retreated some or all of his units.
     *  
     * @param shortMessage
     * @param message
     * @param step
     */
    public void notifyRetreat(String shortMessage, String message, String step, PlayerID retreatingPlayer);

    
    
    
    public void notifyRetreat(GUID battleId, Collection<Unit> retreating);

    /**
     * Show dice for the given battle and step
     * 
     * @param battleId
     * @param dice
     */
    public void notifyDice(GUID battleId, DiceRoll dice, String stepName);

    
    public void gotoBattleStep(GUID battleId, String step);
    
    
}

