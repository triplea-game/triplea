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
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.ui.TripleAFrame;

/**
 * 
 *
 *
 * @author Sean Bridges
 */
public class TripleaDisplay implements ITripleaDisplay
{
    private IDisplayBridge m_displayBridge;
    private final TripleAFrame m_ui;
    
    /**
     * @param ui
     */
    public TripleaDisplay(final TripleAFrame ui)
    {
        m_ui = ui;
    }
    /* 
     * @see games.strategy.engine.display.IDisplay#initialize(games.strategy.engine.display.IDisplayBridge)
     */
    public void initialize(IDisplayBridge bridge)
    {
       m_displayBridge = bridge;
       m_displayBridge.toString();
        
    }
    /* (non-Javadoc)
     * @see games.strategy.triplea.ui.display.ITripleaDisplay#showBattle(games.strategy.net.GUID, java.util.List, games.strategy.engine.data.Territory, java.lang.String, java.util.Collection, java.util.Collection)
     */
    public void showBattle(GUID battleID, Territory location, String battleTitle, Collection attackingUnits, Collection defendingUnits, Map unit_dependents,PlayerID attacker, PlayerID defender)
    {
        m_ui.getBattlePanel().showBattle(battleID,  location, battleTitle,  attackingUnits, defendingUnits, unit_dependents, attacker, defender);
        
    }
    /* (non-Javadoc)
     * @see games.strategy.triplea.ui.display.ITripleaDisplay#listBattleSteps(games.strategy.net.GUID, java.lang.String, java.util.List)
     */
    public void listBattleSteps(GUID battleID, String currentStep, List steps)
    {
       m_ui.getBattlePanel().listBattle(battleID, currentStep, steps);
        
    }
    /* 
     * @see games.strategy.triplea.ui.display.ITripleaDisplay#casualtyNotification(java.lang.String, games.strategy.triplea.delegate.DiceRoll, games.strategy.engine.data.PlayerID, java.util.Collection, java.util.Collection, java.util.Map, boolean)
     */
    public void casualtyNotification(String step, DiceRoll dice, PlayerID player, Collection killed, Collection damaged, Map dependents, boolean autoCalculated)
    {
        m_ui.getBattlePanel().casualtyNotification(step,dice, player, killed, damaged, dependents, autoCalculated);
        
    }
    /* (non-Javadoc)
     * @see games.strategy.triplea.ui.display.ITripleaDisplay#battleEnd(games.strategy.net.GUID, java.lang.String)
     */
    public void battleEnd(GUID battleID, String message)
    {
        m_ui.getBattlePanel().battleEndMessage(battleID, message);
    }
    /* (non-Javadoc)
     * @see games.strategy.triplea.ui.display.ITripleaDisplay#bombingResults(games.strategy.net.GUID, int[], int)
     */
    public void bombingResults(GUID battleID, int[] dice, int cost)
    {
        m_ui.getBattlePanel().bombingResults(battleID, dice, cost);
    }
    
    
    
    
}
