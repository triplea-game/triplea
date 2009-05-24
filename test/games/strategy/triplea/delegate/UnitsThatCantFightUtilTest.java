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

package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.*;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;

import java.util.Collection;

import junit.framework.TestCase;

public class UnitsThatCantFightUtilTest extends TestCase {

    
    
    
    public void testNoSuicideAttacksAA50AtStart()
    {
        //at the start of the game, there are no suicide attacks
        GameData data = LoadGameUtil.loadGame("AA50", "AA50-41.xml");
        
        Collection<Territory> territories = new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
        
        assertTrue(territories.isEmpty());
    }
    
    
    
    public void testSuicideAttackInAA50()
    {
        GameData data = LoadGameUtil.loadGame("AA50", "AA50-41.xml");
        
        //add a german sub to sz 12       
        Territory sz12 = territory("12 Sea Zone", data);
        addTo(
         sz12,
         transports(data).create(1,germans(data))
         );
        
        Collection<Territory> territories = new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
        
        assertTrue(territories.contains(sz12));
    }
    
    public void testSuicideAttackInAA50WithTransportedUnits()
    {
        GameData data = LoadGameUtil.loadGame("AA50", "AA50-41.xml");
        
        //add a german sub to sz 12       
        Territory sz12 = territory("12 Sea Zone", data);
        addTo(
         sz12,
         transports(data).create(1,germans(data))
         );
        
        Collection<Territory> territories = new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
        
        assertTrue(territories.contains(sz12));
    }
    
    
    public void testSuicideAttackInRevised()
    {
        GameData data = LoadGameUtil.loadGame("revised", "revised.xml");
             
        Territory sz15 = territory("15 Sea Zone", data);
        addTo(
         sz15,
         transports(data).create(1,germans(data))
         );
        
        Collection<Territory> territories = new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
        
        assertTrue(territories.contains(sz15));
    }
    
}
