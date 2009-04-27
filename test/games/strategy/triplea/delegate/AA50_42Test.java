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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TestDelegateBridge;
import games.strategy.engine.data.Unit;
import games.strategy.engine.display.IDisplay;
import games.strategy.kingstable.ui.display.DummyDisplay;

import java.util.List;

import junit.framework.TestCase;

public class AA50_42Test extends TestCase {
    
        private GameData m_data;

        @Override
        protected void setUp() throws Exception
        {
            m_data = LoadGameUtil.loadGame("AA50", "AA50-42.xml");
        }

        @Override
        protected void tearDown() throws Exception
        {
            m_data = null;
        }

        private ITestDelegateBridge getDelegateBridge(PlayerID player)
        {
            ITestDelegateBridge bridge1 = new TestDelegateBridge(m_data, player, (IDisplay) new DummyDisplay());
            TestTripleADelegateBridge bridge2 = new TestTripleADelegateBridge(bridge1, m_data);
            return bridge2;
        }
        
      
        public void testTransportAttack()
        {
            Territory sz13 = m_data.getMap().getTerritory("13 Sea Zone");
            Territory sz12 = m_data.getMap().getTerritory("12 Sea Zone");
            
            
            PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");

            MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
            ITestDelegateBridge bridge = getDelegateBridge(germans);
            bridge.setStepName("CombatMove");
            moveDelegate.start(bridge, m_data);

            
            Route sz13To12 = new Route();
            sz13To12.setStart(sz13);
            sz13To12.add(sz12);

            
            List<Unit> transports = sz13.getUnits().getMatches(Matches.UnitIsTransport);
            assertEquals(1, transports.size());
            
            
            String error = moveDelegate.move(transports, sz13To12);
            assertEquals(error,MoveValidator.UNESCORTED_UNITS_WILL_DIE_IN_COMBAT);
            
        }



}
