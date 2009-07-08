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
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.util.DummyTripleAPlayer;
import games.strategy.util.IntegerMap;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
            ITestDelegateBridge bridge = getDelegateBridge(british);
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
            del.start(getDelegateBridge( british(m_data)), m_data);
            
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
            ITestDelegateBridge bridge = getDelegateBridge(british);
            bridge.setStepName("CombatMove");
            moveDelegate.start(bridge, m_data);
            bridge.setRemote(new DummyTripleAPlayer());
            
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
            
            moveDelegate.end();
            
            bridge.setStepName("combat");
            
            BattleDelegate battleDelegate = battleDelegate(m_data);
            battleDelegate.start(bridge, m_data);
            
            assertTrue(battleDelegate.getBattles().isEmpty());
            
        }
        
        public void testLoadedTransportAttackKillsLoadedUnits() 
        {
            PlayerID british = british(m_data);
            
            MoveDelegate moveDelegate = moveDelegate(m_data);
            ITestDelegateBridge bridge = getDelegateBridge(british);
            bridge.setStepName("CombatMove");
            bridge.setRemote(new DummyTripleAPlayer() {

                @Override
                public boolean selectAttackSubs(Territory unitTerritory) { 
                    return true;
                }
            });
            moveDelegate.start(bridge, m_data);
            
            Territory sz9 = territory("9 Sea Zone", m_data);
            Territory sz7 = territory("7 Sea Zone", m_data);
            Territory uk = territory("United Kingdom", m_data);
            
            Route sz9ToSz7 = new Route(
                sz9,
                territory("8 Sea Zone", m_data),
                sz7
            );
            
            //move the transport to attack, this is suicide but valid            
            List<Unit> transports = sz9.getUnits().getMatches(Matches.UnitIsTransport);
            move(transports, sz9ToSz7);
         
            //load the transport
            load(uk.getUnits().getMatches(Matches.UnitIsInfantry), new Route(uk, sz7));
            
            moveDelegate(m_data).end();
                        
            bridge.setStepName("combat");
            
            BattleDelegate battleDelegate = battleDelegate(m_data);
            battleDelegate.start(bridge, m_data);
            
            assertEquals(2,new TransportTracker().transporting(transports.get(0)).size());
            
            //fight the battle
            assertValid(battleDelegate.fightBattle(sz7, false));
            
            //make sure the infantry die with the transport
            assertTrue(sz7.getUnits().toString(), sz7.getUnits().getMatches(Matches.unitOwnedBy(british)).isEmpty());
        }
        
        public void testCanRetreatIntoEmptyEnemyTerritory() 
        {
            Territory eastPoland = territory("East Poland", m_data);
            Territory ukraine = territory("Ukraine", m_data);
            Territory poland = territory("Poland", m_data);
            //remove all units from east poland
            removeFrom(eastPoland, eastPoland.getUnits().getUnits());
            
            
            MoveDelegate moveDelegate = moveDelegate(m_data);
            ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
            delegateBridge.setStepName("CombatMove");
            moveDelegate.start(delegateBridge, m_data);
           
            Territory bulgaria = territory("Bulgaria Romania", m_data);
            
            //attack from bulgraia
            move(bulgaria.getUnits().getUnits(), new Route(bulgaria, ukraine));
            //add an air attack from east poland
            move(poland.getUnits().getMatches(Matches.UnitIsAir), new Route(poland, eastPoland, ukraine));
            
            //we should not be able to retreat to east poland!
            //that territory is still owned by the enemy
            MustFightBattle battle = (MustFightBattle) 
                MoveDelegate.getBattleTracker(m_data).getPendingBattle(ukraine, false);
            
            assertFalse(battle.getAttackerRetreatTerritories().contains(eastPoland));            
        }
        
        public void testCanRetreatIntoBlitzedTerritory() 
        {
            Territory eastPoland = territory("East Poland", m_data);
            Territory ukraine = territory("Ukraine", m_data);
            Territory poland = territory("Poland", m_data);
            //remove all units from east poland
            removeFrom(eastPoland, eastPoland.getUnits().getUnits());
            
            
            MoveDelegate moveDelegate = moveDelegate(m_data);
            ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
            delegateBridge.setStepName("CombatMove");
            moveDelegate.start(delegateBridge, m_data);
           
            Territory bulgaria = territory("Bulgaria Romania", m_data);
            
            //attack from bulgraia
            move(bulgaria.getUnits().getUnits(), new Route(bulgaria, ukraine));
            //add a blitz attack
            move(poland.getUnits().getMatches(Matches.UnitIsArmour), new Route(poland, eastPoland, ukraine));
            
            //we should not be able to retreat to east poland!
            //that territory was just conquered
            MustFightBattle battle = (MustFightBattle) 
                MoveDelegate.getBattleTracker(m_data).getPendingBattle(ukraine, false);
            
            assertTrue(battle.getAttackerRetreatTerritories().contains(eastPoland));            
        }
        
        public void testMechanizedInfantry()
        {
            //Set up tech
            PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
            ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
            TechTracker.addAdvance(germans, m_data, delegateBridge, TechAdvance.MECHANIZED_INFANTRY);

            //Set up the move delegate
            MoveDelegate moveDelegate = moveDelegate(m_data);
            delegateBridge.setStepName("CombatMove");
            moveDelegate.start(delegateBridge, m_data);
            
            //Set up the territories
            Territory poland = territory("Poland", m_data);
            Territory eastPoland = territory("East Poland", m_data);
            Territory belorussia = territory("Belorussia", m_data);
            
            //Set up the unit types
            UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
            
            //Remove all units from east poland
            removeFrom(eastPoland, eastPoland.getUnits().getUnits());
            
            //Get total number of units in territories to start 
            Integer preCountIntPoland = poland.getUnits().size();
            Integer preCountIntBelorussia = belorussia.getUnits().size();
            
            //Get units
            Collection<Unit> moveUnits = poland.getUnits().getUnits(infantryType, 3);
            moveUnits.addAll(poland.getUnits().getMatches(Matches.UnitIsArmour));

            //add a INVALID blitz attack
            String errorResults = moveDelegate.move(moveUnits, new Route(poland, eastPoland, belorussia));
            assertError(errorResults);
            
            //Fix the number of units
            moveUnits.clear();
            moveUnits.addAll(poland.getUnits().getUnits(infantryType, 2));
            moveUnits.addAll(poland.getUnits().getMatches(Matches.UnitIsArmour));
            
            //add a VALID blitz attack
            String validResults = moveDelegate.move(moveUnits, new Route(poland, eastPoland, belorussia));
            assertValid(validResults);
            
            //Get number of units in territories after move (adjusted for movement)
            Integer postCountIntPoland = poland.getUnits().size() +4;
            Integer postCountIntBelorussia = belorussia.getUnits().size() -4;
            
          //Compare the number of units before and after
            assertEquals(preCountIntPoland, postCountIntPoland);
            assertEquals(preCountIntBelorussia, postCountIntBelorussia);            
        }
        

        public void testJetPower()
        {
            //Set up tech
            PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
            ITestDelegateBridge delegateBridge = getDelegateBridge( germans(m_data));
            TechTracker.addAdvance(germans, m_data, delegateBridge, TechAdvance.JET_POWER);
            
            //Set up the territories
            Territory poland = territory("Poland", m_data);
            Territory eastPoland = territory("East Poland", m_data);            

            //Set up the unit types
            UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
            
            delegateBridge.setStepName("germanBattle");
            while(!m_data.getSequence().getStep().getName().equals("germanBattle")) {
                m_data.getSequence().next();
            }

            //With JET_POWER attacking fighter hits on 4 (0 base)
            List<Unit> germanFighter = (List<Unit>) poland.getUnits().getUnits(fighterType, 1);
            delegateBridge.setRandomSource(new ScriptedRandomSource(new int[]
            { 3 }));
            DiceRoll roll1 = DiceRoll.rollDice(germanFighter, false, germans, delegateBridge, m_data, new MockBattle(eastPoland), "");
            assertEquals(1, roll1.getHits());


            //With JET_POWER defending fighter misses on 5 (0 base)
            delegateBridge.setRandomSource(new ScriptedRandomSource(new int[]
            { 4 }));
            DiceRoll roll2 = DiceRoll.rollDice(germanFighter, true, germans, delegateBridge, m_data, new MockBattle(eastPoland), "");
            assertEquals(0, roll2.getHits());
        }
       
        public void testBidPlace() 
        {
            ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
            bridge.setStepName("placeBid");
            bidPlaceDelegate(m_data).start(bridge, m_data);
            
            //create 20 british infantry
            addTo(british(m_data), infantry(m_data).create(20, british(m_data)));
            
            Territory uk = territory("United Kingdom", m_data);
            Collection<Unit> units = british(m_data).getUnits().getUnits();
            PlaceableUnits placeable = bidPlaceDelegate(m_data).getPlaceableUnits(units, uk);
            assertEquals(20, placeable.getMaxUnits());
            assertNull(placeable.getErrorMessage());
            
            String error = bidPlaceDelegate(m_data).placeUnits(units, uk);
            assertNull(error);    
        }

        public void testFactoryPlace() throws Exception
        {
            //get the xml file (needed because it has "heldUnits" meaning already purchased)
            URL url = this.getClass().getResource("DelegateTest.xml");
            
            InputStream input= url.openStream();
            m_data = (new GameParser()).parse(input);
            input.close();
            
            //Set up game
            PlayerID british = m_data.getPlayerList().getPlayerID("British");
            ITestDelegateBridge delegateBridge = getDelegateBridge( british(m_data));
                        
            //Set up the territories
            Territory egypt = territory("Anglo Sudan Egypt", m_data);        

            //Set up the unit types
            UnitType factoryType = m_data.getUnitTypeList().getUnitType("factory");

            //Set up the move delegate
            PlaceDelegate placeDelegate = placeDelegate(m_data);
            delegateBridge.setStepName("Place");
            delegateBridge.setPlayerID(british);
            placeDelegate.start(delegateBridge, m_data);
            
            //Add the factory
            IntegerMap<UnitType> map = new IntegerMap<UnitType>();
            map.add(factoryType, 1);
            
            //Place the factory
            String response = placeDelegate.placeUnits(getUnits(map, british), egypt);      
            assertValid(response);
            
            //placeUnits performPlace
            //get production and unit production values
            TerritoryAttachment ta = TerritoryAttachment.get(egypt);            
            assertEquals(ta.getUnitProduction(), ta.getProduction());            
        }
       
        
        public void testMoveUnitsThroughSubs()
        {
            ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
            bridge.setStepName("nonCombatMove");
            moveDelegate(m_data).start(bridge, m_data);
            
            Territory sz6 = territory("6 Sea Zone", m_data);
            Route route = new Route(
                sz6,
                territory("7 Sea Zone", m_data),
                territory("8 Sea Zone", m_data)
                );
            
            String error = moveDelegate(m_data).move(sz6.getUnits().getUnits(), route);
            assertNull(error,error);
        }
        
        public void testMoveUnitsThroughTransports()
        {
            ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
            bridge.setStepName("nonCombatMove");
            moveDelegate(m_data).start(bridge, m_data);
            
            Territory sz12 = territory("12 Sea Zone", m_data);
            Route route = new Route(
                sz12,
                territory("13 Sea Zone", m_data),
                territory("14 Sea Zone", m_data)
                );
            
            String error = moveDelegate(m_data).move(sz12.getUnits().getUnits(), route);
            assertNull(error,error);
        }
            
        public void testLoadThroughSubs()
        {
            ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
            bridge.setStepName("nonCombatMove");
            MoveDelegate moveDelegate = moveDelegate(m_data);
            moveDelegate.start(bridge, m_data);
            
            Territory sz8 = territory("8 Sea Zone", m_data);
            Territory sz7 = territory("7 Sea Zone", m_data);
            Territory sz6 = territory("6 Sea Zone", m_data);
            Territory uk = territory("United Kingdom", m_data);
            
            
            //add a transport
            addTo(sz8, transports(m_data).create(1,british(m_data)));
            
            //move the transport where to the sub is
            assertValid(moveDelegate.move(sz8.getUnits().getUnits(), new Route(sz8,sz7)));
            
            
            //load the transport
            load(uk.getUnits().getMatches(Matches.UnitIsInfantry), new Route(uk, sz7));
                        
            //move the transport out
            assertValid(moveDelegate.move(sz7.getUnits().getMatches(Matches.unitOwnedBy(british(m_data))), 
                new Route(sz7,sz6)));
        }

        
        public void testAttackUndoAndAttackAgain() 
        {
            MoveDelegate move = moveDelegate(m_data);
            ITestDelegateBridge bridge = getDelegateBridge(italians(m_data));
            bridge.setStepName("CombatMove");
            move.start(bridge, m_data);
            
            Territory sz14 = territory("14 Sea Zone", m_data);
            Territory sz13 = territory("13 Sea Zone", m_data);
            Territory sz12 = territory("12 Sea Zone", m_data);
            
            Route r = new Route(sz14,sz13,sz12);
            
            //move the battleship
            move(sz14.getUnits().getMatches(Matches.UnitIsTwoHit), r );
            
            //move everything
            move(sz14.getUnits().getMatches(Matches.UnitIsNotTransport), r );
            //undo it
            move.undoMove(1);
                        
            //move again
            move(sz14.getUnits().getMatches(Matches.UnitIsNotTransport), r );
            
            MustFightBattle mfb = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(sz12, false);
            
            //only 2 attacking units
            assertEquals(3, mfb.getAttackingUnits().size());
        }
        

        /***********************************************************/
        /***********************************************************/
        /***********************************************************/
        /***********************************************************/
        /*
         * Add Utilities here
         */
        
        private Collection<Unit> getUnits(IntegerMap<UnitType> units, PlayerID from)
        {
            Iterator<UnitType> iter = units.keySet().iterator();
            Collection rVal = new ArrayList(units.totalValues());
            while(iter.hasNext())
            {
                UnitType type = iter.next();
                rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
            }
            return rVal;
        }


        /***********************************************************/
        /***********************************************************/
        /***********************************************************/
        /***********************************************************/
        /*
         * Add assertions here
         */
        public void assertValid(String string)
        {
            assertNull(string,string);
        }
        
        public void assertError(String string)
        {
            assertNotNull(string,string);
        }
}

