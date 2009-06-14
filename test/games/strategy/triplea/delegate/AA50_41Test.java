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
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TestDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.kingstable.ui.display.DummyDisplay;
import junit.framework.TestCase;

public class AA50_41Test extends TestCase {
    
        private GameData m_data;

        @Override
        protected void setUp() throws Exception
        {
            m_data = LoadGameUtil.loadGame("AA50", "AA50-41.xml");
        }

        @Override
        protected void tearDown() throws Exception
        {
            m_data = null;
        }

      
        public void testDefendingTrasnportsAutoKilled()
        {
            Territory sz13 = m_data.getMap().getTerritory("13 Sea Zone");
            Territory sz12 = m_data.getMap().getTerritory("12 Sea Zone");
            
            
            PlayerID british = m_data.getPlayerList().getPlayerID("British");
            
            MoveDelegate moveDelegate = moveDelegate(m_data);
            ITestDelegateBridge bridge = getDelegateBridge(m_data,british);
            bridge.setStepName("CombatMove");
            moveDelegate.start(bridge, m_data);
            
          
            
            Route sz12To13 = new Route();
            sz12To13.setStart(sz12);
            sz12To13.add(sz13);

            
            String error = moveDelegate.move(sz12.getUnits().getUnits(), sz12To13);
            assertEquals(error,null);
            
            
            assertEquals(sz13.getUnits().size(), 3);
            
            moveDelegate.end();
            
            //the transport was not removed automatically
            assertEquals(sz13.getUnits().size(), 3);
            
            BattleDelegate bd = battleDelegate(m_data);
            assertFalse(bd.getBattleTracker().getPendingBattleSites(false).isEmpty());
            
        }
        

        
        public void testUnplacedDie() 
        {
            PlaceDelegate del = placeDelegate(m_data);
            del.start(getDelegateBridge(m_data, british(m_data)), m_data);
            
            addTo(british(m_data), 
                  transports(m_data).create(1,british(m_data)));
            
            del.end();
            
            //unplaced units die
            assertEquals(1, british(m_data).getUnits().size());        
        }
        
        
        public void testInfantryLoadOnlyTransports() 
        {
            
            
            Territory gibraltar = territory("Gibraltar", m_data);
            //add a tank to gibralter
            PlayerID british = british(m_data);
            addTo(gibraltar, infantry(m_data).create(1,british));
            
            MoveDelegate moveDelegate = moveDelegate(m_data);
            ITestDelegateBridge bridge = getDelegateBridge(m_data,british);
            bridge.setStepName("CombatMove");
            moveDelegate.start(bridge, m_data);
            
            Territory sz9 = territory("9 Sea Zone", m_data);
            Territory sz13 = territory("13 Sea Zone", m_data);
            Route sz9ToSz13 = new Route(
                sz9,
                territory("12 Sea Zone", m_data),
                sz13
            );
            
            //move the transport to attack, this is suicide but valid
            move(sz9.getUnits().getMatches(Matches.UnitIsTransport), sz9ToSz13);
            
         
            //load the transport
            load(gibraltar.getUnits().getUnits(), new Route(gibraltar, sz13));
            
            
            //not sure what to do here
            //should the load have suceeded?
            //should there be a battle
            //currently it is wrong, because the 
            //infantry is counted as an attacking unit
            //and kills the transport
            fail();
            
        }
}
