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

package games.strategy.triplea.baseAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.LoadGameUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class AIUtilsTest extends TestCase
{
    
    private GameData m_data;

    @Override
    protected void setUp() throws Exception
    {
        m_data = LoadGameUtil.loadGame("revised", "revised.xml");
    }

    @Override
    protected void tearDown() throws Exception
    {
        m_data = null;
    }

    
    public void testCost()
    {
        UnitType infantry = m_data.getUnitTypeList().getUnitType("infantry");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        
        assertEquals(3, AIUtils.getCost(infantry, british, m_data));
        
    }
    
    public void testSortByCost()
    {
        Territory germany = m_data.getMap().getTerritory("Germany");
        List<Unit> sorted = new ArrayList<Unit>(germany.getUnits().getUnits());
        Collections.sort(sorted, AIUtils.getCostComparator());
        
        assertEquals(sorted.get(0).getUnitType().getName(), "infantry");
        
    }
}
