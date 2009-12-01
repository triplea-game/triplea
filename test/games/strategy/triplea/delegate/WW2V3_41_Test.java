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

import static games.strategy.triplea.delegate.BattleStepStrings.*;
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
import games.strategy.net.GUID;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.ui.display.DummyDisplay;
import games.strategy.triplea.util.DummyTripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.IntegerMap;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;


public class WW2V3_41_Test extends TestCase {
    
        private GameData m_data;
        
        @Override
        protected void setUp() throws Exception
        {
            m_data = LoadGameUtil.loadGame("AA50", "ww2v3_1941.xml");
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
        
        
        public void testPlaceEmpty() 
        {
            PlaceDelegate del = placeDelegate(m_data);
            del.start(getDelegateBridge( british(m_data)), m_data);
            
            addTo(british(m_data), 
                  transports(m_data).create(1,british(m_data)));
            
            String error = del.placeUnits(Collections.EMPTY_LIST, territory("United Kingdom", m_data));
            
            assertNull(error);
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
            move(poland.getUnits().getMatches(Matches.UnitCanBlitz), new Route(poland, eastPoland, ukraine));
            
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
            moveUnits.addAll(poland.getUnits().getMatches(Matches.UnitCanBlitz));

            //add a INVALID blitz attack
            String errorResults = moveDelegate.move(moveUnits, new Route(poland, eastPoland, belorussia));
            assertError(errorResults);
            
            //Fix the number of units
            moveUnits.clear();
            moveUnits.addAll(poland.getUnits().getUnits(infantryType, 2));
            moveUnits.addAll(poland.getUnits().getMatches(Matches.UnitCanBlitz));
            
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
            
            //only 3 attacking units
            //the battleship and the two cruisers
            assertEquals(3, mfb.getAttackingUnits().size());
        }
        
        
        public void testAttackSubsOnSubs() 
        {
            String defender = "Germans";
            String attacker = "British";
            
            Territory attacked = territory("31 Sea Zone", m_data);
            Territory from = territory("32 Sea Zone", m_data);
            
            //1 sub attacks 1 sub
            addTo(from, submarine(m_data).create(1,british(m_data)));
            addTo(attacked, submarine(m_data).create(1,germans(m_data)));
            
            ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);
            
            move(from.getUnits().getUnits(), new Route(from,attacked));
           
            moveDelegate(m_data).end();
            
            MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
            
            List<String> steps = battle.determineStepStrings(true, bridge);
            assertEquals(                
                Arrays.asList(
                    defender + SUBS_SUBMERGE,
                    attacker + SUBS_SUBMERGE,
                    attacker + SUBS_FIRE,
                    defender + SELECT_SUB_CASUALTIES,                    
                    defender + SUBS_FIRE,
                    attacker + SELECT_SUB_CASUALTIES,
                    REMOVE_SNEAK_ATTACK_CASUALTIES,
                    REMOVE_CASUALTIES,
                    attacker + ATTACKER_WITHDRAW
                ).toString(),
                steps.toString()
            );
            
          
            
            bridge.setRemote(new DummyTripleAPlayer());
            
            //fight, each sub should fire
            //and hit
            ScriptedRandomSource randomSource = new ScriptedRandomSource(
                0,0,ScriptedRandomSource.ERROR);
            bridge.setRandomSource(randomSource);
            battle.fight(bridge);
            
            assertEquals(2, randomSource.getTotalRolled());
            assertTrue(attacked.getUnits().isEmpty());

        }
        
        public void testAttackSubsOnDestroyer() 
        {
            String defender = "Germans";
            String attacker = "British";
            
            Territory attacked = territory("31 Sea Zone", m_data);
            Territory from = territory("32 Sea Zone", m_data);
            
            //1 sub attacks 1 sub and 1 destroyer
            //defender sneak attacks, not attacker
            addTo(from, submarine(m_data).create(1,british(m_data)));
            addTo(attacked, submarine(m_data).create(1,germans(m_data)));
            addTo(attacked, destroyer(m_data).create(1,germans(m_data)));
            
            ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);
            
            move(from.getUnits().getUnits(), new Route(from,attacked));
           
            moveDelegate(m_data).end();
            
            MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
            
            List<String> steps = battle.determineStepStrings(true, bridge);
            assertEquals(                
                Arrays.asList(
                    defender + SUBS_SUBMERGE,   
                    
                    defender + SUBS_FIRE,
                    attacker + SELECT_SUB_CASUALTIES,
                    
                    REMOVE_SNEAK_ATTACK_CASUALTIES,
                    
                    attacker + SUBS_FIRE,
                    defender + SELECT_SUB_CASUALTIES,
                    
                    defender + FIRE,
                    attacker + SELECT_CASUALTIES,
                    REMOVE_CASUALTIES,
                    attacker + ATTACKER_WITHDRAW
                ).toString(),
                steps.toString()
            );
            
          
            
            bridge.setRemote(new DummyTripleAPlayer());
            
            //defending subs sneak attack and hit
            //no chance to return fire
            ScriptedRandomSource randomSource = new ScriptedRandomSource(
                0,ScriptedRandomSource.ERROR);
            bridge.setRandomSource(randomSource);
            battle.fight(bridge);
            
            assertEquals(1, randomSource.getTotalRolled());
            assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(british(m_data))).isEmpty());
            assertEquals(2, attacked.getUnits().size());
            
        }

        public void testAttackDestroyerAndSubsAgainstSub() 
        {
            String defender = "Germans";
            String attacker = "British";
            
            Territory attacked = territory("31 Sea Zone", m_data);
            Territory from = territory("32 Sea Zone", m_data);
            
            //1 sub and 1 destroyer attack 1 sub
            //defender sneak attacks, not attacker
            addTo(from, submarine(m_data).create(1,british(m_data)));
            addTo(from, destroyer(m_data).create(1,british(m_data)));
            addTo(attacked, submarine(m_data).create(1,germans(m_data)));
            
            ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);
            
            move(from.getUnits().getUnits(), new Route(from,attacked));
           
            moveDelegate(m_data).end();
            
            MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
            
            List<String> steps = battle.determineStepStrings(true, bridge);
            assertEquals(                
                Arrays.asList(
                    attacker + SUBS_SUBMERGE,
                    
                    attacker + SUBS_FIRE,
                    defender + SELECT_SUB_CASUALTIES,

                    REMOVE_SNEAK_ATTACK_CASUALTIES,
                    
                    defender + SUBS_FIRE,
                    attacker + SELECT_SUB_CASUALTIES,
                    
                    attacker + FIRE,
                    defender + SELECT_CASUALTIES,
                    
                    REMOVE_CASUALTIES,
                    attacker + ATTACKER_WITHDRAW
                ).toString(),
                steps.toString()
            );
            
            bridge.setRemote(new DummyTripleAPlayer());
            
            //attacking subs sneak attack and hit
            //no chance to return fire
            ScriptedRandomSource randomSource = new ScriptedRandomSource(
                0,ScriptedRandomSource.ERROR);
            bridge.setRandomSource(randomSource);
            battle.fight(bridge);
            
            assertEquals(1, randomSource.getTotalRolled());
            assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(germans(m_data))).isEmpty());
            assertEquals(2, attacked.getUnits().size());
        }

        

        public void testAttackDestroyerAndSubsAgainstSubAndDestroyer() 
        {
            String defender = "Germans";
            String attacker = "British";
            
            Territory attacked = territory("31 Sea Zone", m_data);
            Territory from = territory("32 Sea Zone", m_data);
            
            //1 sub and 1 destroyer attack 1 sub and 1 destroyer
            //no sneak attacks
            addTo(from, submarine(m_data).create(1,british(m_data)));
            addTo(from, destroyer(m_data).create(1,british(m_data)));
            addTo(attacked, submarine(m_data).create(1,germans(m_data)));
            addTo(attacked, destroyer(m_data).create(1,germans(m_data)));
            
            ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);
            
            move(from.getUnits().getUnits(), new Route(from,attacked));
           
            moveDelegate(m_data).end();
            
            MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
            
            List<String> steps = battle.determineStepStrings(true, bridge);
            assertEquals(                
                Arrays.asList(
                    
                    
                    attacker + SUBS_FIRE,
                    defender + SELECT_SUB_CASUALTIES,
                    defender + SUBS_FIRE,
                    attacker + SELECT_SUB_CASUALTIES,

                    attacker + FIRE,
                    defender + SELECT_CASUALTIES,
                    
                    defender + FIRE,
                    attacker + SELECT_CASUALTIES,
                    
                    REMOVE_CASUALTIES,
                    attacker + ATTACKER_WITHDRAW
                ).toString(),
                steps.toString()
            );
            
            bridge.setRemote(new DummyTripleAPlayer(){

                @Override
                public CasualtyDetails selectCasualties(Collection<Unit> selectFrom,
                    Map<Unit, Collection<Unit>> dependents, int count, String message,
                    DiceRoll dice, PlayerID hit, List<Unit> defaultCasualties, GUID battleID) {
                    
                    return new CasualtyDetails(
                        Arrays.asList(selectFrom.iterator().next()),
                        Collections.<Unit>emptyList(), false);
                }
                
            });
            
            //attacking subs sneak attack and hit
            //no chance to return fire
            ScriptedRandomSource randomSource = new ScriptedRandomSource(
                0,0,0,0,ScriptedRandomSource.ERROR);
            bridge.setRandomSource(randomSource);
            battle.fight(bridge);
            
            assertEquals(4, randomSource.getTotalRolled());            
            assertEquals(0, attacked.getUnits().size());
        }

        public void testLimitBombardtoNumberOfUnloaded() 
        {
            MoveDelegate move = moveDelegate(m_data);
            ITestDelegateBridge bridge = getDelegateBridge(italians(m_data));
            bridge.setRemote(new DummyTripleAPlayer()
            {
                @Override
                public boolean selectShoreBombard(Territory unitTerritory) {
                    return true;
                }
            });
            bridge.setStepName("CombatMove");
            move.start(bridge, m_data);
            
            Territory sz14 = territory("14 Sea Zone", m_data);
            Territory sz15 = territory("15 Sea Zone", m_data);
            Territory eg = territory("Egypt", m_data);
            Territory li = territory("Libya", m_data);
            Territory balkans = territory("Balkans", m_data);
            
            
            //load the transports
            load(balkans.getUnits().getMatches(Matches.UnitIsInfantry), new Route(balkans,sz14));
            
            //move the fleet
            move(sz14.getUnits().getUnits(), new Route(sz14,sz15));
            
            //move troops from Libya
            move(li.getUnits().getMatches(Matches.unitOwnedBy(italians(m_data))), new Route(li, eg));
  
            //unload the transports
            move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15,eg));
            
            move.end();
            
            //start the battle phase, this will ask the user to bombard
            battleDelegate(m_data).start(bridge,m_data);
            
            MustFightBattle mfb = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(eg, false);
            
            
            //only 2 battleships are allowed to bombard
            //Currently no units will bombard so test will fail.
            assertEquals(2, mfb.getBombardingUnits().size());
        }
        
        public void testAmphAttackUndoAndAttackAgainBombard() 
        {
            MoveDelegate move = moveDelegate(m_data);
            ITestDelegateBridge bridge = getDelegateBridge(italians(m_data));
            bridge.setRemote(new DummyTripleAPlayer()
            {
                @Override
                public boolean selectShoreBombard(Territory unitTerritory) {
                    return true;
                }
            });
            bridge.setStepName("CombatMove");
            move.start(bridge, m_data);
            
            Territory sz14 = territory("14 Sea Zone", m_data);
            Territory sz15 = territory("15 Sea Zone", m_data);
            Territory eg = territory("Egypt", m_data);
            Territory li = territory("Libya", m_data);
            Territory balkans = territory("Balkans", m_data);
            
            //load the transports
            load(balkans.getUnits().getMatches(Matches.UnitIsInfantry), new Route(balkans,sz14));
            
            //move the fleet
            move(sz14.getUnits().getUnits(), new Route(sz14,sz15));
            
            //move troops from Libya
            move(li.getUnits().getMatches(Matches.unitOwnedBy(italians(m_data))), new Route(li, eg));
  
            //unload the transports
            move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15,eg));
            
            //undo amphibious landing
            move.undoMove(move.getMovesMade().size() -1);
                        
            //move again
            move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15,eg) );
            
            move.end();
            
            //start the battle phase, this will ask the user to bombard
            battleDelegate(m_data).start(bridge,m_data);
            
            MustFightBattle mfb = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(eg, false);
            
            
            //only 2 battleships are allowed to bombard
            //Currently no units will bombard so test will fail.
            assertEquals(2, mfb.getBombardingUnits().size());
        }
        
        
        public void testCarrierWithAlliedPlanes() 
        {
        	Territory sz8 = territory("8 Sea Zone", m_data);
        	Territory sz1 = territory("1 Sea Zone", m_data);
        	
        	addTo(sz8, carrier(m_data).create(1, british(m_data)));
        	addTo(sz8, fighter(m_data).create(1, americans(m_data)));
        	
        	Route route = new Route(sz8, sz1);
        	
        	
            ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);
            
            move(sz8.getUnits().getUnits(), route);
            
            //make sure the fighter moved
            assertTrue(sz8.getUnits().getUnits().isEmpty());
            assertFalse(sz1.getUnits().getMatches(Matches.UnitIsAir).isEmpty());
 
        }
        
        public void testAirCanLandWithAlliedFighters() 
        {
        	
        	//germany owns madagascar, with 1 fighter in it
        	//also 1 carrier, and 1 allied fighter in sz 40
        	//the fighter should not be able to move from madagascar 
        	//to sz 40, since with the allied fighter, their is no room
        	//on the carrier
        	
        	Territory madagascar = territory("French Madagascar", m_data);
        	PlayerID germans = germans(m_data);
			madagascar.setOwner(germans);
        	
        	Territory sz40 = territory("40 Sea Zone", m_data);
        	addTo(sz40, carrier(m_data).create(1, germans));
        	addTo(sz40, fighter(m_data).create(1, italians(m_data)));
        	addTo(madagascar, fighter(m_data).create(1, germans));
        	
        	Route route = m_data.getMap().getRoute(madagascar, sz40);
        	
            ITestDelegateBridge bridge = getDelegateBridge(germans);
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);
            //don't allow kamikazee
            bridge.setRemote(new DummyTripleAPlayer() {
				@Override
				public boolean confirmMoveKamikaze() {
					return false;
				}            	
            });
        	
            String error =  moveDelegate(m_data).move(madagascar.getUnits().getUnits(), route);
            assertError(error);
        }
        
        public void testMechInfSimple() 
        {
        	PlayerID germans = germans(m_data);
        	Territory france = territory("France", m_data);
        	Territory germany = territory("Germany", m_data);
        	Territory poland = territory("Poland", m_data);
        	
			TechAttachment.get(germans).setMechanizedInfantry("true");
			
			ITestDelegateBridge bridge = getDelegateBridge(germans);
	        bridge.setStepName("CombatMove");
	        
	        moveDelegate(germans.getData()).start(bridge, germans.getData());
			
			Route r = new Route(france,germany,poland);
			List<Unit> toMove = new ArrayList<Unit>();
			
			//1 armour and 1 infantry
			toMove.addAll(france.getUnits().getMatches(Matches.UnitCanBlitz));
			toMove.add(france.getUnits().getMatches(Matches.UnitIsInfantry).get(0));
			
			move(toMove, r);
						
        }
        
        public void testMechInfUnitAlreadyMovedSimple() 
        {
        	PlayerID germans = germans(m_data);
        	Territory france = territory("France", m_data);
        	Territory germany = territory("Germany", m_data);
        	
			TechAttachment.get(germans).setMechanizedInfantry("true");
			
			ITestDelegateBridge bridge = getDelegateBridge(germans);
	        bridge.setStepName("CombatMove");
	        moveDelegate(germans.getData()).start(bridge, germans.getData());
			
			//get rid of the infantry in france
			removeFrom(france, france.getUnits().getMatches(Matches.UnitIsInfantry));
			
			//move an infantry from germany to france
			move(germany.getUnits().getMatches(Matches.UnitIsInfantry).subList(0, 1), new Route(germany,france));			
	
			//try to move all the units in france, the infantry should not be able to move			
			Route r = new Route(france,germany);
			String error = moveDelegate(m_data).move(france.getUnits().getUnits(), r);
			
			assertFalse(error == null);
						
        }
        
        public void testParatroopsWalkOnWater() 
        {
        	
        	//paratroops should stop on water
        	
        	PlayerID germans = germans(m_data);
        	Territory france = territory("France", m_data);
			TechAttachment.get(germans).setParatroopers("true");
			
			Route r = m_data.getMap().getRoute(france, territory("7 Sea Zone", m_data));
			Collection<Unit> paratroopers = france.getUnits().getMatches(Matches.UnitIsParatroop);
			assertFalse(paratroopers.isEmpty());
			
			MoveValidationResult results = MoveValidator.validateMove(
					paratroopers, 
					r, germans, Collections.<Unit>emptyList(), false, null, m_data);
			assertFalse(results.isMoveValid());
			
        }
        
        public void testMoveParatroopersAsNonPartroops() 
        {
        	
        	//move a bomber and a paratrooper
        	//one step, but as a normal movement
        	
        	PlayerID germans = germans(m_data);
        	Territory germany = territory("Germany", m_data);
        	Territory nwe = territory("Northwestern Europe", m_data);
        	
        	ITestDelegateBridge bridge = getDelegateBridge(germans);
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);
        	
			TechAttachment.get(germans).setParatroopers("true");
			
			List<Unit> paratrooper = germany.getUnits().getMatches(Matches.UnitIsParatroop);
			paratrooper = paratrooper.subList(0,1);
			List<Unit> bomberAndParatroop = new ArrayList<Unit>(paratrooper);
			bomberAndParatroop.addAll(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber));
			
			//move to nwe, this is a valid move, and it not a partroop move
			move(bomberAndParatroop, new Route(germany, nwe));
			
        } 
        
        public void testCantMoveParatroopersThatMovedPreviously() 
        {
        	//make sure infantry can't be moved as paratroopers after moving
        	
        	PlayerID germans = germans(m_data);
        	Territory germany = territory("Germany", m_data);
        	Territory nwe = territory("Northwestern Europe", m_data);
        	Territory poland = territory("Poland", m_data);
        	Territory eastPoland = territory("East Poland", m_data);        	
        	
        	ITestDelegateBridge bridge = getDelegateBridge(germans);
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);
        	
			TechAttachment.get(germans).setParatroopers("true");
			
			List<Unit> paratrooper = nwe.getUnits().getMatches(Matches.UnitIsParatroop);
			
			move(paratrooper, new Route(nwe,germany));
			
			List<Unit> bomberAndParatroop = new ArrayList<Unit>(paratrooper);
			bomberAndParatroop.addAll(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber));
			
			//move the units to east poland
			String error = moveDelegate(m_data).move(bomberAndParatroop, new Route(germany, poland, eastPoland));
			
			assertFalse(error == null);
			
        }        
        
        public void testParatroopersMoveTwice() 
        {
        	//After a battle move to put a bomber + infantry (paratroop) in a first enemy
        	//territory, you can make a new move (in the same battle move round) to put
        	//bomber+ infantry in a more internal enemy territory.
        	
        	
        	PlayerID germans = germans(m_data);
        	Territory germany = territory("Germany", m_data);
        	Territory poland = territory("Poland", m_data);
        	Territory eastPoland = territory("East Poland", m_data);
        	Territory beloRussia = territory("Belorussia", m_data);
        	
        	ITestDelegateBridge bridge = getDelegateBridge(germans);
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);
        	
			TechAttachment.get(germans).setParatroopers("true");
			
			List<Unit> paratrooper = germany.getUnits().getMatches(Matches.UnitIsParatroop);
			paratrooper = paratrooper.subList(0, 1);
			List<Unit> bomberAndParatroop = new ArrayList<Unit>(paratrooper);
			bomberAndParatroop.addAll(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber));
			
			//move the units to east poland
			move(bomberAndParatroop, new Route(germany, poland, eastPoland));
			
			//try to move them further, this should fail
			String error = moveDelegate(m_data).move(bomberAndParatroop, new Route(eastPoland, beloRussia));
			
			
			assertFalse(error == null);
			
        }
        
        public void testAmphibAttackWithPlanesOnlyAskRetreatOnce() 
        {
        	PlayerID germans = germans(m_data);
        	ITestDelegateBridge bridge = getDelegateBridge(germans);
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);

            Territory france = territory("France", m_data);
            Territory egypt = territory("Egypt", m_data);
            Territory balkans = territory("Balkans", m_data);
            Territory libya = territory("Libya", m_data);
            Territory germany = territory("Germany", m_data);
			Territory sz13 = territory("13 Sea Zone", m_data);
			Territory sz14 = territory("14 Sea Zone", m_data);
			Territory sz15 = territory("15 Sea Zone", m_data);

			bridge.setRemote(new DummyTripleAPlayer() {
				@Override
				public Territory retreatQuery(GUID battleID, boolean submerge,
						Collection<Territory> possibleTerritories,
						String message) {
					assertFalse(message.contains(MustFightBattle.RETREAT_PLANES));
					return null;
				}				
			});
			
			bridge.setDisplay(new DummyDisplay() {

				@Override
				public void listBattleSteps(GUID battleID, List<String> steps) {
					for(String s : steps) {
						assertFalse(s.contains(BattleStepStrings.PLANES_WITHDRAW));
					}
				}} );
			
            //move units for amphib assault			
			load(france.getUnits().getMatches(Matches.UnitIsInfantry),
            		new Route(france, sz13));            
			move(sz13.getUnits().getUnits(), new Route(sz13,sz14,sz15));
			move(sz15.getUnits().getMatches(Matches.UnitIsInfantry), new Route(sz15, egypt));
			
			//ground attack
			load(libya.getUnits().getMatches(Matches.UnitIsArtillery),
            		new Route(libya, egypt));
			
			//air units
			move(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber), new Route(germany, balkans, sz14, sz15, egypt));
        	
			
			moveDelegate(m_data).end();
			
			bridge.setStepName("Combat");
	
			//cook the dice so that all miss first round,all hit second round
			bridge.setRandomSource(new ScriptedRandomSource(5,5,5,5,5,5,5,5,5,1,1,1,1,1,1,1,1,1));
			battleDelegate(m_data).start(bridge, m_data);
			
			battleDelegate(m_data).fightBattle(egypt, false);
			
        }
        
        
        public void testDefencelessTransportsDie() 
        {        	
        	PlayerID british = british(m_data);
        	
        	ITestDelegateBridge bridge = getDelegateBridge(british);
            bridge.setStepName("CombatMove");
            moveDelegate(m_data).start(bridge, m_data);
            
            Territory uk = territory("United Kingdom", m_data);
            Territory sz5 = territory("5 Sea Zone", m_data);

            //remove the sub
            removeFrom(sz5, sz5.getUnits().getMatches(Matches.UnitIsSub));
            
            bridge.setRemote(new DummyTripleAPlayer() {
				@Override
				public Territory retreatQuery(GUID battleID, boolean submerge,
						Collection<Territory> possibleTerritories,
						String message) {
					//we should not be asked to retreat
					throw new IllegalStateException("Should not be asked to retreat:" + message);					
				}				
			});

            move(uk.getUnits().getMatches(Matches.UnitIsAir), m_data.getMap().getRoute(uk, sz5) );
			
            //move units for amphib assault			
			moveDelegate(m_data).end();
			
			bridge.setStepName("Combat");
	
			//cook the dice so that 1 british fighters hits, and nothing else
			//this will leave 1 transport alone in the sea zone
			bridge.setRandomSource(new ScriptedRandomSource(1,5,5,5,5,5,5,5,5));
			battleDelegate(m_data).start(bridge, m_data);
			
			battleDelegate(m_data).fightBattle(sz5, false);
			
			//make sure the transports died
			assertTrue(sz5.getUnits().getMatches(Matches.unitIsOwnedBy(germans(m_data))).isEmpty());
			
        }
        

        public void testFighterLandsWhereCarrierCanBePlaced() 
        {        	        
        	PlayerID germans = germans(m_data);
        	
        	//germans have 1 carrier to place
			addTo(germans, carrier(m_data).create(1, germans));
			
			//start the move phase
			ITestDelegateBridge bridge = getDelegateBridge(germans);
	            bridge.setStepName("CombatMove");
	            moveDelegate(m_data).start(bridge, m_data);
		
	        bridge.setRemote(new DummyTripleAPlayer(){

				@Override
				public boolean confirmMoveHariKari() {
					return false;
				}});
	            
			//the fighter should be able to move and hover in the sea zone
	        //the fighter has no movement left
			Territory neEurope = territory("Northwestern Europe", m_data);
			Route route = new Route(
					neEurope,
					territory("Germany", m_data),
					territory("Poland", m_data),
					territory("Baltic States", m_data),
					territory("5 Sea Zone", m_data)
					);
			
			
			//the fighter should be able to move, and hover in the sea zone until the carrier is placed
			move(neEurope.getUnits().getMatches(Matches.UnitIsAir), route);
        	
        }

        public void testFighterCantHoverWithNoCarrierToPlace() 
        {
        	        	
			//start the move phase
			ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
	            bridge.setStepName("CombatMove");
	            moveDelegate(m_data).start(bridge, m_data);

            bridge.setRemote(new DummyTripleAPlayer(){

				@Override
				public boolean confirmMoveHariKari() {
					return false;
				}});
        	
			//the fighter should not be able to move and hover in the sea zone
	        //since their are no carriers to place
		    //the fighter has no movement left	            
			Territory neEurope = territory("Northwestern Europe", m_data);
			Route route = new Route(
					neEurope,
					territory("Germany", m_data),
					territory("Poland", m_data),
					territory("Baltic States", m_data),
					territory("5 Sea Zone", m_data)
					);
			
		 	String error = moveDelegate(m_data).move(neEurope.getUnits().getMatches(Matches.UnitIsAir), route);
		 	assertNotNull(error);
        	
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
            Collection<Unit> rVal = new ArrayList<Unit>(units.totalValues());
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

