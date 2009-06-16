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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Utility for detecting and removing units that can't land at the end of a phase. 
 */
public class UnitsThatCantFightUtil
{
    private final GameData m_data;    
    
    public UnitsThatCantFightUtil(final GameData data)
    {
        m_data = data;        
    }

    //TODO Used to notify of kamikazi attacks
    public Collection<Territory> getTerritoriesWhereUnitsCantFight(PlayerID player)
    {
        CompositeMatch<Unit> enemyAttackUnits = new CompositeMatchAnd<Unit>();
        enemyAttackUnits.add(Matches.enemyUnit(player, m_data));
        enemyAttackUnits.add(Matches.unitCanAttack(player));
        
        Collection<Territory> cantFight = new ArrayList<Territory>();
        for (Territory current : m_data.getMap())
        { 
            //get all owned non-combat units
            CompositeMatch<Unit> ownedUnitsMatch = new CompositeMatchAnd<Unit>();
            ownedUnitsMatch.add(new InverseMatch<Unit>(Matches.UnitIsAAOrFactory));
            if(current.isWater()) {
                ownedUnitsMatch.add(Matches.UnitIsLand.invert());
            }
            ownedUnitsMatch.add(Matches.unitIsOwnedBy(player));

            //All owned units
            int countAllOwnedUnits = current.getUnits().countMatches(ownedUnitsMatch);
            
            //only noncombat units
            ownedUnitsMatch.add(new InverseMatch<Unit>(Matches.unitCanAttack(player)));
            Collection<Unit> nonCombatUnits = current.getUnits().getMatches(ownedUnitsMatch);
          
            if(nonCombatUnits.isEmpty() || nonCombatUnits.size() != countAllOwnedUnits)
                continue;
            
            if(current.getUnits().someMatch(enemyAttackUnits))
                cantFight.add(current);
        }
        return cantFight;
    }
  
}
