/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.InverseMatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * Utility for detecting and removing units that can't land at the end of a phase. 
 */
public class UnitsThatCantFightUtil
{
    private final GameData m_data;
    private final IDelegateBridge m_bridge;
    
    public UnitsThatCantFightUtil(final GameData data, final IDelegateBridge bridge)
    {
        m_data = data;
        m_bridge = bridge;
    }

  //TODO Used to notify of kamikazi attacks
    public Collection<Territory> getTerritoriesWhereUnitsCantFight(PlayerID player)
    {
        Collection<Territory> cantFight = new ArrayList<Territory>();
        Iterator<Territory> territories = m_data.getMap().getTerritories().iterator();
        
        while (territories.hasNext())
        {
            Territory current = (Territory) territories.next();
            //get all owned non-combat units
            CompositeMatch<Unit> ownedUnitsMatch = new CompositeMatchAnd<Unit>();
        	ownedUnitsMatch.add(new InverseMatch<Unit>(Matches.UnitIsAAOrFactory));
        	ownedUnitsMatch.add(Matches.unitIsOwnedBy(player));

        	//All owned units
            int countAllOwnedUnits = current.getUnits().countMatches(ownedUnitsMatch);
            //only noncombat units
        	ownedUnitsMatch.add(new InverseMatch<Unit>(Matches.unitCanAttack(player)));
            Collection<Unit> nonCombatUnits = current.getUnits().getMatches(ownedUnitsMatch);
                        
            if(!nonCombatUnits.isEmpty() && nonCombatUnits.size() == countAllOwnedUnits)
            	cantFight.add(current);
        }
        return cantFight;
    }

    public void removeUnitsThatCantFight(PlayerID player)
    {
        Iterator<Territory> territories = getTerritoriesWhereUnitsCantFight(player).iterator();
        while (territories.hasNext())
        {
            Territory current = territories.next();
            
          //get all owned non-combat units
            CompositeMatch<Unit> ownedUnitsMatch = new CompositeMatchAnd<Unit>();
        	ownedUnitsMatch.add(new InverseMatch<Unit>(Matches.UnitIsAAOrFactory));
        	ownedUnitsMatch.add(Matches.unitIsOwnedBy(player));

        	//All owned units
            int countAllOwnedUnits = current.getUnits().countMatches(ownedUnitsMatch);
            //only noncombat units
        	ownedUnitsMatch.add(new InverseMatch<Unit>(Matches.unitCanAttack(player)));
            Collection<Unit> nonCombatUnits = current.getUnits().getMatches(ownedUnitsMatch);
            
            //Match for enemy combat units
            CompositeMatch<Unit> enemyUnitsMatch = new CompositeMatchAnd<Unit>();
        	enemyUnitsMatch.add(Matches.enemyUnit(player, m_data));
        	enemyUnitsMatch.add(Matches.unitCanAttack(player));
            
        	//Are there any nonCombatants and enemy combat units
        	//TODO perhaps ignore if there are units that can be ignored (subs)
            if (nonCombatUnits.size() != 0 && nonCombatUnits.size() == countAllOwnedUnits && current.getUnits().someMatch(enemyUnitsMatch))
            {	
            	//Get the pending battle
            	Battle nonBombingBattle = MoveDelegate.getBattleTracker(m_data).getPendingBattle(current, false);
            	//Get the dependent units
            	if(nonBombingBattle != null)
            	{
            		nonCombatUnits.addAll(nonBombingBattle.getDependentUnits(nonCombatUnits));

            		//Kill the nonCombat units and their dependents
            		removeUnitsThatCantFight(player, current, nonCombatUnits);

            		//Remove the battle from the stack
            		Route route = new Route();
            		route.setStart(current);            	
            		nonBombingBattle.removeAttack(route, nonCombatUnits);
            	}
            }
        }
    }

    private void removeUnitsThatCantFight(PlayerID player, Territory territory, Collection<Unit> units)
    {
        Collection<Unit> toRemove = new ArrayList<Unit>(units.size());
        
        toRemove.addAll(units);        

        Change remove = ChangeFactory.removeUnits(territory, toRemove);

        String transcriptText = MyFormatter.unitsToTextNoOwner(toRemove) + " could not fight in " + territory.getName() + " and "
                + (toRemove.size() > 1 ? "were" : "was") + " removed";
        m_bridge.getHistoryWriter().startEvent(transcriptText);

        m_bridge.addChange(remove);

    }  
}
