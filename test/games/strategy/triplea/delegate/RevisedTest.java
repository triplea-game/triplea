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
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.util.DummyTripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class RevisedTest extends TestCase 
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
    
    public void testMoveBadRoute() 
    {
       PlayerID british = m_data.getPlayerList().getPlayerID("British");
        
       Territory sz1 = m_data.getMap().getTerritory("1 Sea Zone");
       Territory sz11 = m_data.getMap().getTerritory("11 Sea Zone");
       Territory sz9 = m_data.getMap().getTerritory("9 Sea Zone");
                
       
       ITestDelegateBridge bridge = getDelegateBridge(british);
       bridge.setStepName("NonCombatMove");
       moveDelegate(m_data).start(bridge, m_data);

       String error = moveDelegate(m_data).move(sz1.getUnits().getUnits(), new Route(sz1,sz11,sz9));
        assertTrue(error != null);
    }
    
    
    public void testAlliedNeighbors() 
    {
        PlayerID americans = americans(m_data);
        Territory centralUs = territory("Central United States", m_data);
        Set<Territory> enemyNeighbors = m_data.getMap().getNeighbors(
            centralUs, Matches.isTerritoryEnemy(americans, m_data));        
        assertTrue(enemyNeighbors.isEmpty());
    }
    
    public void testSubAdvance()
    {
        UnitType sub = m_data.getUnitTypeList().getUnitType("submarine");
        UnitAttachment attachment = UnitAttachment.get(sub);
        
        PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        
        //before the advance, subs defend and attack at 2
        assertEquals(2, attachment.getDefense(japanese));
        assertEquals(2, attachment.getAttack(japanese));
        
        ITestDelegateBridge bridge = getDelegateBridge(japanese);
        
        TechTracker.addAdvance(japanese, m_data, bridge, TechAdvance.SUPER_SUBS);
        
        
        //after tech advance, this is now 3
        assertEquals(2, attachment.getDefense(japanese));
        assertEquals(3, attachment.getAttack(japanese));
    }
    
    public void testMoveThroughSubmergedSubs() 
    {
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        
        Territory sz1 = m_data.getMap().getTerritory("1 Sea Zone");
        Territory sz7 = m_data.getMap().getTerritory("7 Sea Zone");
        Territory sz8 = m_data.getMap().getTerritory("8 Sea Zone");
                
        TripleAUnit sub = (TripleAUnit) sz8.getUnits().iterator().next();
        sub.setSubmerged(true);
        
        //now move to attack it
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        
        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setStepName("NonCombatMove");
        moveDelegate.start(bridge, m_data);
        
        //the transport can enter sz 8
        //since the sub is submerged
        Route m1 = new Route(sz1,sz8);        
        assertNull(moveDelegate.move(sz1.getUnits().getUnits(), m1));
        
        
        //the transport can now leave sz8
        Route m2 = new Route(sz8,sz7);
        
        String error = moveDelegate.move(sz8.getUnits().getMatches(Matches.unitIsOwnedBy(british)), m2);
        assertNull(error,error);
    }
    
    public void testRetreatBug()
    {        
        PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
        PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
        
        ITestDelegateBridge bridge = getDelegateBridge(russians);

        //we need to initialize the original owner
        InitializationDelegate initDel = (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
        initDel.start(bridge, m_data);
        initDel.end();

        
        //make sinkian japanese owned, put one infantry in it
        Territory sinkiang = m_data.getMap().getTerritory("Sinkiang");
        new ChangePerformer(m_data).perform(ChangeFactory.removeUnits(sinkiang, sinkiang.getUnits().getUnits()));
        
        PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        sinkiang.setOwner(japanese);

        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sinkiang, infantryType.create(1, japanese)));
        
        
        
        //now move to attack it
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);
        Territory novo = m_data.getMap().getTerritory("Novosibirsk");
        moveDelegate.move(novo.getUnits().getUnits(), m_data.getMap().getRoute(novo, sinkiang));
        
        
        moveDelegate.end();
        
        BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
        battle.start(bridge, m_data);
        
        //fight the battle
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {0,0,0}));
        bridge.setRemote(getDummyPlayer());
        battle.fightBattle(sinkiang, false);
        
        
        battle.end();
        
        assertEquals(sinkiang.getOwner(), americans);
        assertTrue(battle.getBattleTracker().wasConquered(sinkiang));
        
        bridge.setStepName("NonCombatMove");
        moveDelegate.start(bridge, m_data);
        
        Territory russia = m_data.getMap().getTerritory("Russia");
        
        
        //move two tanks from russia, then undo
        Route r = new Route();
        r.setStart(russia);
        r.add(novo);
        r.add(sinkiang);
        assertNull(moveDelegate.move(russia.getUnits().getMatches(Matches.UnitCanBlitz), r) );
        
        
        moveDelegate.undoMove(0);
        
        assertTrue(battle.getBattleTracker().wasConquered(sinkiang));
        
        //now move the planes into the territory
        assertNull(moveDelegate.move(russia.getUnits().getMatches(Matches.UnitIsAir), r) );
        //make sure they can't land, they can't because the territory was conquered
        assertEquals(1, moveDelegate.getTerritoriesWhereAirCantLand().size());
    }
    

    public void testContinuedBattles()
    {
        PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        
        ITestDelegateBridge bridge = getDelegateBridge(germans);
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        //set up battle
        Territory germany = m_data.getMap().getTerritory("Germany");
        Territory karelia = m_data.getMap().getTerritory("Karelia S.S.R.");
        Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
        new ChangePerformer(m_data).perform(ChangeFactory.removeUnits(sz5, sz5.getUnits().getUnits()));

        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        UnitType subType = m_data.getUnitTypeList().getUnitType("submarine");
        UnitType trnType = m_data.getUnitTypeList().getUnitType("transport");
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz5, subType.create(1, germans)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz5, trnType.create(1, germans)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz5, subType.create(1, russians)));
        
        //submerge the russian sub        
        TripleAUnit sub = (TripleAUnit) Match.getMatches(sz5.getUnits().getUnits(), Matches.unitIsOwnedBy(russians)).iterator().next();
        sub.setSubmerged(true);
                        
        //now move an infantry through the sz
        String results = moveDelegate.move(Match.getNMatches(germany.getUnits().getUnits(), 1, Matches.unitIsOfType(infantryType)), m_data.getMap().getRoute(germany, sz5), Match.getMatches(sz5.getUnits().getUnits(), Matches.unitIsOfType(trnType)));
        assertNull(results);
        results = moveDelegate.move(Match.getNMatches(sz5.getUnits().getUnits(), 1, Matches.unitIsOfType(infantryType)), m_data.getMap().getRoute(sz5, karelia));
        assertNull(results);
        
        moveDelegate.end();
        
        BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
        battle.start(bridge, m_data);
        
        BattleTracker tracker = MoveDelegate.getBattleTracker(m_data);
        
        //The battle should NOT be empty
        assertTrue(tracker.hasPendingBattle(sz5, false));        
        assertFalse(tracker.getPendingBattle(sz5, false).isEmpty());
                
        battle.end();
    }
    
    
    public void testLoadAlliedTransports() 
    {
        PlayerID british = british(m_data);
        PlayerID americans = americans(m_data);
        
        Territory uk = territory("United Kingdom", m_data);
        
        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setStepName("CombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        
        
        //create 2 us infantry
        addTo(uk, infantry(m_data).create(2, americans));
        
        //try to load them on the british players turn
        Territory sz2 = territory("2 Sea Zone", m_data);
        String error = moveDelegate(m_data).move(
            uk.getUnits().getMatches(Matches.unitIsOwnedBy(americans)), 
            new Route(uk, sz2), 
            sz2.getUnits().getMatches(Matches.UnitIsTransport));
        
        // should not be able to load on british turn, only on american turn
        assertFalse(error == null);
        
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
    
    public void testBombingRaid() 
    {
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);
        bridge.setRemote(new DummyTripleAPlayer(){

            @Override
            public boolean shouldBomberBomb(Territory territory) {
               return true;
            }
        });
        
        Territory uk = territory("United Kingdom", m_data);
        Territory germany = territory("Germany", m_data);
        Route route = new Route(
            uk,
            territory("6 Sea Zone", m_data),
            territory("5 Sea Zone", m_data),
            germany
            );
        
        String error = moveDelegate.move(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber),
            route);
        assertNull(error);

        BattleTracker tracker = MoveDelegate.getBattleTracker(m_data);
        //there should be a bombing battle
        assertTrue(tracker.hasPendingBattle(germany, true));
        //there should not be a normal battle
        assertFalse(tracker.hasPendingBattle(germany, false));
        
        //start the battle phase, this should not add a new battle
        moveDelegate.end();        
        battleDelegate(m_data).start(bridge, m_data);
        
        //there should be a bombing battle
        assertTrue(tracker.hasPendingBattle(germany, true));
        //there should not be a normal battle
        assertFalse(tracker.hasPendingBattle(germany, false));
        
        
    }
    
    public void testOverFlyBombersDies()
    {
        PlayerID british = british(m_data);
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        bridge.setRemote(new DummyTripleAPlayer()
        {
            @Override
            public boolean confirmMoveInFaceOfAA(Collection<Territory> aaFiringTerritories) {
               return true;
            }     
        });
        bridge.setRandomSource(new ScriptedRandomSource(0));

        Territory uk = territory("United Kingdom", m_data);
        Territory we = territory("Western Europe", m_data);
        Territory se = territory("Southern Europe", m_data);

        Route route = new Route(
            uk,territory("7 Sea Zone", m_data), we,se
            );
        
        move(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber), route);
        
        //the aa gun should have fired. the bomber no longer exists
        assertTrue(se.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
        assertTrue(we.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
        assertTrue(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty()); 
    }

    public void testMultipleOverFlyBombersDies()
    {
        PlayerID british = british(m_data);
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        bridge.setRemote(new DummyTripleAPlayer()
        {
            @Override
            public boolean confirmMoveInFaceOfAA(Collection<Territory> aaFiringTerritories) {
               return true;
            }     
        });
        bridge.setRandomSource(new ScriptedRandomSource(0,4));

        Territory uk = territory("United Kingdom", m_data);
        Territory sz7 = territory("7 Sea Zone", m_data);
        Territory we = territory("Western Europe", m_data);
        Territory se = territory("Southern Europe", m_data);
        Territory balk = territory("Balkans", m_data);
        addTo(uk, bomber(m_data).create(1,british));

        Route route = new Route(uk, sz7, we, se, balk);

        move(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber), route);
        
        //the aa gun should have fired (one hit, one miss in each territory overflown). the bombers no longer exists
        assertTrue(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty()); 
        assertTrue(we.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
        assertTrue(se.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
        assertTrue(balk.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty()); 
    }
    
    public void testOverFlyBombersJoiningBattleDie()
    {

        //a bomber flies over aa to join a battle, gets hit,
        //it should not appear in the battle
        
        PlayerID british = british(m_data);
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        bridge.setRemote(new DummyTripleAPlayer()
        {
            @Override
            public boolean confirmMoveInFaceOfAA(Collection<Territory> aaFiringTerritories) {
               return true;
            }     
        });
        bridge.setRandomSource(new ScriptedRandomSource(0));

        Territory uk = territory("United Kingdom", m_data);
        Territory we = territory("Western Europe", m_data);
        Territory se = territory("Southern Europe", m_data);
        Territory sz14 = territory("14 Sea Zone", m_data);
        Territory sz15 = territory("15 Sea Zone", m_data);
        Territory egypt = territory("Anglo Egypt", m_data);

        //start a battle in se
        removeFrom(sz14, sz14.getUnits().getUnits());
        addTo(sz15, transports(m_data).create(1,british));
        
        load(egypt.getUnits().getMatches(Matches.UnitIsInfantry), new Route(egypt,sz15));
        
        move(sz15.getUnits().getUnits(), new Route(sz15,sz14));
        
        move(sz14.getUnits().getMatches(Matches.UnitIsInfantry), new Route(sz14,se));
        
        Route route = new Route(
            uk, territory("7 Sea Zone", m_data),we,se
            );
        
        move(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber), route);
        
        //the aa gun should have fired and hit
        assertTrue(se.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
        assertTrue(we.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
        assertTrue(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());  
    }
    
    public void testTransportAttack()
    {
        Territory sz14 = m_data.getMap().getTerritory("14 Sea Zone");
        Territory sz13 = m_data.getMap().getTerritory("13 Sea Zone");
        
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");

        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(germans);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        
        Route sz14To13 = new Route();
        sz14To13.setStart(sz14);
        sz14To13.add(sz13);

        
        List<Unit> transports = sz14.getUnits().getMatches(Matches.UnitIsTransport);
        assertEquals(1, transports.size());
        
        
        String error = moveDelegate.move(transports, sz14To13);
        assertNull(error,error);
        
    }
    
    public void testLoadUndo()
    {
        Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
        Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
        
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");

        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(germans);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        
        Route eeToSz5 = new Route();
        eeToSz5.setStart(eastEurope);
        eeToSz5.add(sz5);

        //load the transport in the baltic
        List<Unit> infantry = eastEurope.getUnits().getMatches(Matches.unitIsOfType(infantryType));
        assertEquals(2, infantry.size());
        
        TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
        
        String error = moveDelegate.move(infantry, eeToSz5, Collections.<Unit>singletonList(transport));
        assertNull(error,error);
        
        //make sure the transport was loaded
        assertTrue(moveDelegate.getMovesMade().get(0).wasTransportLoaded(transport));

        
        
        //make sure it was laoded
        assertTrue(transport.getTransporting().containsAll(infantry));
        assertTrue(((TripleAUnit) infantry.get(0)).getWasLoadedThisTurn());
        
        //udo the move
        moveDelegate.undoMove(0);
        
        //make sure that loaded is not set
        assertTrue(transport.getTransporting().isEmpty());
        assertFalse(((TripleAUnit) infantry.get(0)).getWasLoadedThisTurn());

        
    }
    
    
    public void testLoadDependencies()
    {
     
        Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
        Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
        Territory norway = m_data.getMap().getTerritory("Norway");
        
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");

        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(germans);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        
        Route eeToSz5 = new Route();
        eeToSz5.setStart(eastEurope);
        eeToSz5.add(sz5);

        //load the transport in the baltic
        List<Unit> infantry = eastEurope.getUnits().getMatches(Matches.unitIsOfType(infantryType));
        assertEquals(2, infantry.size());
        
        TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
        
        //load the transport
        String error = moveDelegate.move(infantry, eeToSz5, Collections.<Unit>singletonList(transport));
        assertNull(error,error);
        
        
        Route sz5ToNorway = new Route();
        sz5ToNorway.setStart(sz5);
        sz5ToNorway.add(norway);

        //move the infantry in two steps
        error = moveDelegate.move(infantry.subList(0,1), sz5ToNorway);
        assertNull(error);
        error = moveDelegate.move(infantry.subList(1,2), sz5ToNorway);
        assertNull(error);
        
        assertEquals(3, moveDelegate.getMovesMade().size());
      
        //the load
        UndoableMove move1 = moveDelegate.getMovesMade().get(0);
        
        //the first unload
        AbstractUndoableMove move2 = moveDelegate.getMovesMade().get(0);
        
        //the second unload must be done first
        assertFalse(move1.getcanUndo());
        
        error = moveDelegate.undoMove(2);
        assertNull(error);

        //the second unload must be done first
        assertFalse(move1.getcanUndo());
        
        error = moveDelegate.undoMove(1);
        assertNull(error);
        
        //we can now be undone
        assertTrue(move1.getcanUndo());
    }
    
    public void testLoadUndoInWrongOrder()
    {
        Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
        Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
        
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");

        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(germans);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        
        Route eeToSz5 = new Route();
        eeToSz5.setStart(eastEurope);
        eeToSz5.add(sz5);

        //load the transport in the baltic
        List<Unit> infantry = eastEurope.getUnits().getMatches(Matches.unitIsOfType(infantryType));
        assertEquals(2, infantry.size());
        
        TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
        
        //load the transports
        //in two moves
        String error = moveDelegate.move(infantry.subList(0,1), eeToSz5, Collections.<Unit>singletonList(transport));
        assertNull(error,error);
        error = moveDelegate.move(infantry.subList(1,2), eeToSz5, Collections.<Unit>singletonList(transport));
        assertNull(error,error);
        
        
        //make sure the transport was loaded
        assertTrue(moveDelegate.getMovesMade().get(0).wasTransportLoaded(transport));
        assertTrue(moveDelegate.getMovesMade().get(1).wasTransportLoaded(transport));
        
        
        //udo the moves in reverse order
        moveDelegate.undoMove(0);
        moveDelegate.undoMove(0);
        
        
        //make sure that loaded is not set
        assertTrue(transport.getTransporting().isEmpty());
        assertFalse(((TripleAUnit) infantry.get(0)).getWasLoadedThisTurn());
    }
    
    
    public void testLoadUnloadAlliedTransport() 
    {
        //you cant load and unload an allied transport the same turn
        
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        
        Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
        //add japanese infantry to eastern europe
        PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        Change change = ChangeFactory.addUnits(eastEurope, infantryType.create(1, japanese));
        new ChangePerformer(m_data).perform(change);
        
        
        Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
        
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(japanese);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        
        Route eeToSz5 = new Route();
        eeToSz5.setStart(eastEurope);
        eeToSz5.add(sz5);

        //load the transport in the baltic
        List<Unit> infantry = eastEurope.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitIsOfType(infantryType), Matches.unitIsOwnedBy(japanese)));
        assertEquals(1, infantry.size());
        
        TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
        
        String error = moveDelegate.move(infantry, eeToSz5, Collections.<Unit>singletonList(transport));
        assertNull(error,error);
     
        //try to unload
        Route sz5ToEee = new Route();
        sz5ToEee.setStart(sz5);
        sz5ToEee.add(eastEurope);
        
        error = moveDelegate.move(infantry, sz5ToEee);
        assertEquals(MoveValidator.CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND, error);
    }
    
    public void testUnloadMultipleTerritories()
    {
        //in revised a transport may only unload to 1 territory.
        
        Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
        Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
        
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");

        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(germans);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        
        Route eeToSz5 = new Route();
        eeToSz5.setStart(eastEurope);
        eeToSz5.add(sz5);

        //load the transport in the baltic
        List<Unit> infantry = eastEurope.getUnits().getMatches(Matches.unitIsOfType(infantryType));
        assertEquals(2, infantry.size());
        
        TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
        
        String error = moveDelegate.move(infantry, eeToSz5, Collections.<Unit>singletonList(transport));
        assertNull(error,error);

        
        
        //unload one infantry to Norway
        Territory norway = m_data.getMap().getTerritory("Norway");
        Route sz5ToNorway = new Route();
        sz5ToNorway.setStart(sz5);
        sz5ToNorway.add(norway);
        error = moveDelegate.move(infantry.subList(0, 1), sz5ToNorway);
        assertNull(error,error);

        //make sure the transport was unloaded
        assertTrue(moveDelegate.getMovesMade().get(1).wasTransportUnloaded(transport));

        
        //try to unload the other infantry somewhere else, an error occurs
        Route sz5ToEE = new Route();
        sz5ToEE.setStart(sz5);
        sz5ToEE.add(eastEurope);
        
        error = moveDelegate.move(infantry.subList(1, 2), sz5ToEE);
        assertNotNull(error,error);
        assertTrue(error.startsWith(MoveValidator.TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO));
        
        //end the round
        moveDelegate.end();
        bridge.setStepName("NonCombatMove");
        moveDelegate.start(bridge, m_data);
        moveDelegate.end();
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);
        
        //a new round, the move should work
        moveDelegate.start(bridge, m_data);
        error = moveDelegate.move(infantry.subList(1, 2), sz5ToEE);
        assertNull(error);
    }
    
    public void testUnloadInPreviousPhase()
    {        
        //a transport may not unload in both combat and non combat
        
        Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
        Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
        
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");

        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(germans);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);

        
        Route eeToSz5 = new Route();
        eeToSz5.setStart(eastEurope);
        eeToSz5.add(sz5);

        //load the transport in the baltic
        List<Unit> infantry = eastEurope.getUnits().getMatches(Matches.unitIsOfType(infantryType));
        assertEquals(2, infantry.size());
        
        TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
        
        String error = moveDelegate.move(infantry, eeToSz5, Collections.<Unit>singletonList(transport));
        assertNull(error,error);

        
        
        //unload one infantry to Norway
        Territory norway = m_data.getMap().getTerritory("Norway");
        Route sz5ToNorway = new Route();
        sz5ToNorway.setStart(sz5);
        sz5ToNorway.add(norway);
        error = moveDelegate.move(infantry.subList(0, 1), sz5ToNorway);
        assertNull(error,error);

        assertTrue( ((TripleAUnit) infantry.get(0)).getWasUnloadedInCombatPhase());
        
        //start non combat
        moveDelegate.end();
        bridge.setStepName("germanNonCombatMove");
        //the transport tracker relies on the step name
        while(!m_data.getSequence().getStep().getName().equals("germanNonCombatMove")) {
            m_data.getSequence().next();
        }
        moveDelegate.start(bridge, m_data);
        
        //try to unload the other infantry somewhere else, an error occurs
        error = moveDelegate.move(infantry.subList(1, 2), sz5ToNorway);
        assertNotNull(error,error);
        assertTrue(error.startsWith(MoveValidator.TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE));
    }
    
    public void testSubAttackTransportNonCombat() 
    {
    	Territory sz1 = territory("1 Sea Zone",m_data);
    	
    	Territory sz8 = territory("8 Sea Zone",m_data);
    	PlayerID germans = germans(m_data);
    	
    	//german sub tries to attack a transport in non combat
    	//should be an error
    	MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(germans);
        bridge.setStepName("NonCombatMove");
        moveDelegate.start(bridge, m_data);

        String error = moveDelegate(m_data).move(sz8.getUnits().getUnits(), new Route(sz8,sz1));
        assertError(error);
        
    }
    
    public void testSubAttackNonCombat() 
    {
    	Territory sz2 = territory("2 Sea Zone",m_data);
    	
    	Territory sz8 = territory("8 Sea Zone",m_data);    	
    	PlayerID germans = germans(m_data);
    	
    	//german sub tries to attack a transport in non combat
    	//should be an error
    	MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(germans);
        bridge.setStepName("NonCombatMove");
        moveDelegate.start(bridge, m_data);

        String error = moveDelegate(m_data).move(sz8.getUnits().getUnits(), new Route(sz8,sz2));
        assertError(error);
        
    }
    
    public void testTransportAttackSubNonCombat() 
    {
    	Territory sz1 = territory("1 Sea Zone",m_data);
    	
    	Territory sz8 = territory("8 Sea Zone",m_data);
    	PlayerID british = british(m_data);
    	
    	//german sub tries to attack a transport in non combat
    	//should be an error
    	MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setStepName("NonCombatMove");
        moveDelegate.start(bridge, m_data);

        String error = moveDelegate(m_data).move(sz8.getUnits().getUnits(), new Route(sz1,sz8));
        assertError(error);
        
    }
    
    public void testMoveSubAwayFromSubmergedSubsInBattleZone()
    {
        Territory sz45 = m_data.getMap().getTerritory("45 Sea Zone");
        Territory sz50 = m_data.getMap().getTerritory("50 Sea Zone");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");

        //put 1 british sub in sz 45, this simulates a submerged enemy sub
        UnitType sub = m_data.getUnitTypeList().getUnitType("submarine");
        Change c = ChangeFactory.addUnits(sz45, sub.create(1, british));
        new ChangePerformer(m_data).perform(c);
        
        
        //new move delegate
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        ITestDelegateBridge bridge = getDelegateBridge(japanese);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);


        //move a fighter into the sea zone, this will cause a battle
        Route sz50To45 = new Route();
        sz50To45.setStart(sz50);
        sz50To45.add(sz45);
        String error = moveDelegate.move(sz50.getUnits().getMatches(Matches.UnitIsAir), sz50To45);
        assertNull(error);
        
        assertEquals(1, MoveDelegate.getBattleTracker(m_data).getPendingBattleSites(false).size());
        
        
        //we should be able to move the sub out of the sz
        Route sz45To50 = new Route();
        sz45To50.setStart(sz45);
        sz45To50.add(sz50);
        
        List<Unit> japSub = sz45.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitIsSub, Matches.unitIsOwnedBy(japanese)));
        error = moveDelegate.move(japSub, sz45To50);
        //make sure no error
        assertNull(error);
        //make sure the battle is still there
        assertEquals(1, MoveDelegate.getBattleTracker(m_data).getPendingBattleSites(false).size());
        
        
        //we should be able to undo the move of the sub
        error = moveDelegate.undoMove(1);
        assertNull(error);
        
        //undo the move of the fighter, should be no battles now
        error = moveDelegate.undoMove(0);
        assertNull(error);
        assertEquals(0, MoveDelegate.getBattleTracker(m_data).getPendingBattleSites(false).size());
    }

    public void testAAOwnership()       
    {        	
    	//Set up players
    	PlayerID british = m_data.getPlayerList().getPlayerID("British");
    	PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
    	PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");

    	//Set up the territories
    	Territory india = territory("India", m_data);
    	Territory fic = territory("French Indochina", m_data);
    	Territory china = territory("China", m_data);
    	Territory kwang = territory("Kwantung", m_data);

    	//Preset units in FIC
        UnitType infType = m_data.getUnitTypeList().getUnitType("infantry");
        UnitType aaType = m_data.getUnitTypeList().getUnitType("aaGun");
    	removeFrom(fic, fic.getUnits().getUnits());    	
        addTo(fic, aaGun(m_data).create(1,japanese));	
        addTo(fic, infantry(m_data).create(1,japanese));
    	assertEquals(2, fic.getUnits().getUnitCount());
    	
    	//Get attacking units
    	Collection<Unit> britishUnits = india.getUnits().getUnits(infType, 1);
    	Collection<Unit> japaneseUnits = kwang.getUnits().getUnits(infType, 1);
    	Collection<Unit> americanUnits = china.getUnits().getUnits(infType, 1);
    	
    	//Get Owner prior to battle
    	assertTrue(fic.getUnits().allMatch(Matches.unitIsOwnedBy(japanese(m_data))));    	
    	String preOwner = fic.getOwner().getName();
    	assertEquals(preOwner, "Japanese");

    	//Set up the move delegate
    	ITestDelegateBridge delegateBridge = getDelegateBridge(british(m_data));
    	MoveDelegate moveDelegate = moveDelegate(m_data);
    	delegateBridge.setStepName("CombatMove");
    	moveDelegate.start(delegateBridge, m_data);

    	/*
    	 * add a VALID BRITISH attack
    	 */
    	String validResults = moveDelegate.move(britishUnits, new Route(india, fic));
    	assertValid(validResults);
        moveDelegate(m_data).end();

    	//Set up battle        
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(fic, false);
    	
        delegateBridge.setRemote(new DummyTripleAPlayer());
        
        //fight
        ScriptedRandomSource randomSource = new ScriptedRandomSource(0,5);
        delegateBridge.setRandomSource(randomSource);
        battle.fight(delegateBridge);
 
    	//Get Owner after to battle
    	assertTrue(fic.getUnits().allMatch(Matches.unitIsOwnedBy(british(m_data))));
    	String postOwner = fic.getOwner().getName();
    	assertEquals(postOwner, "British");
    
    	
    	/*
    	 * add a VALID JAPANESE attack
    	 */
    	//Set up battle  
    	delegateBridge = getDelegateBridge(japanese(m_data));
    	delegateBridge.setStepName("CombatMove");
    	moveDelegate.start(delegateBridge, m_data);
    	
    	//Move to battle
    	validResults = moveDelegate.move(japaneseUnits, new Route(kwang, fic));
    	assertValid(validResults);
        moveDelegate(m_data).end();
      
        battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(fic, false);
    	
        delegateBridge.setRemote(new DummyTripleAPlayer());
        
        //fight
        randomSource = new ScriptedRandomSource(0,5);
        delegateBridge.setRandomSource(randomSource);
        battle.fight(delegateBridge);
 
    	//Get Owner after to battle
    	assertTrue(fic.getUnits().allMatch(Matches.unitIsOwnedBy(japanese(m_data))));
    	String midOwner = fic.getOwner().getName();
    	assertEquals(midOwner, "Japanese");

    	
    	/*
    	 * add a VALID AMERICAN attack
    	 */
    	//Set up battle  
    	delegateBridge = getDelegateBridge(americans(m_data));
    	delegateBridge.setStepName("CombatMove");
    	moveDelegate.start(delegateBridge, m_data);
    	
    	//Move to battle
    	validResults = moveDelegate.move(americanUnits, new Route(china, fic));
    	assertValid(validResults);
        moveDelegate(m_data).end();
      
        battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(fic, false);
    	
        delegateBridge.setRemote(new DummyTripleAPlayer());
        
        //fight
        randomSource = new ScriptedRandomSource(0,5);
        delegateBridge.setRandomSource(randomSource);
        battle.fight(delegateBridge);
 
    	//Get Owner after to battle
    	assertTrue(fic.getUnits().allMatch(Matches.unitIsOwnedBy(americans(m_data))));
    	String endOwner = fic.getOwner().getName();
    	assertEquals(endOwner, "Americans");
    }
    
    public void testStratBombCasualties()
    {
        Territory germany = m_data.getMap().getTerritory("Germany");
        Territory uk = m_data.getMap().getTerritory("United Kingdom");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        
        
        BattleTracker tracker = new BattleTracker();
        StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, germans,  tracker );
        
        List<Unit> bombers = uk.getUnits().getMatches(Matches.UnitIsStrategicBomber);
        addTo(germany, bombers);
		battle.addAttackChange(m_data.getMap().getRoute(uk, germany), bombers);

        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setRemote(getDummyPlayer());
        //aa guns rolls 0 and hits
        bridge.setRandomSource(new ScriptedRandomSource( new int[] {0, ScriptedRandomSource.ERROR} ));

        
        //int PUsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        int pusBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        
        battle.fight(bridge);
        
        //int PUsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        int pusAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        assertEquals(pusBeforeRaid, pusAfterRaid);
        
        assertEquals(0,germany.getUnits().getMatches(Matches.unitIsOwnedBy(british)).size());
    }
    
    public void testStratBombCasualtiesLowLuck()
    {
    	makeGameLowLuck(m_data);
        Territory germany = m_data.getMap().getTerritory("Germany");
        Territory uk = m_data.getMap().getTerritory("United Kingdom");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        
        
        BattleTracker tracker = new BattleTracker();
        StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, germans,  tracker );
        
        List<Unit> bombers = bomber(m_data).create(2, british);
        addTo(germany, bombers);
		battle.addAttackChange(m_data.getMap().getRoute(uk, germany), bombers);

        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setRemote(getDummyPlayer());
        // should be exactly 3 rolls total.  would be exactly 2 rolls if the number of units being shot at = max dice side of the AA gun, because the casualty selection roll would not happen in LL
        // first 0 is the AA gun rolling 1@2 and getting a 1, which is a hit
        // second 0 is the LL AA casualty selection randomly picking the first unit to die
        // third 0 is the single remaining bomber dealing 1 damage to the enemy's PUs
        bridge.setRandomSource(new ScriptedRandomSource( new int[] {0, 0, 0, ScriptedRandomSource.ERROR} ));

        int pusBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        
        battle.fight(bridge);
        
        int pusAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        assertEquals(pusBeforeRaid - 1, pusAfterRaid);
        
        assertEquals(1,germany.getUnits().getMatches(Matches.unitIsOwnedBy(british)).size());
    }
    
    public void testStratBombCasualtiesLowLuckManyBombers()
    {
    	makeGameLowLuck(m_data);
        Territory germany = m_data.getMap().getTerritory("Germany");
        Territory uk = m_data.getMap().getTerritory("United Kingdom");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        
        
        BattleTracker tracker = new BattleTracker();
        StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, germans,  tracker );
        
        List<Unit> bombers = bomber(m_data).create(7, british);
        addTo(germany, bombers);
		battle.addAttackChange(m_data.getMap().getRoute(uk, germany), bombers);

        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setRemote(getDummyPlayer());
        //aa guns rolls 0 and hits, next 5 dice are for the bombing raid cost for the 
        //surviving bombers
        bridge.setRandomSource(new ScriptedRandomSource( new int[] {0, 0, 0, 0, 0, 0, ScriptedRandomSource.ERROR} ));

        int pusBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        battle.fight(bridge);
        int pusAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        assertEquals(pusBeforeRaid - 5, pusAfterRaid);

        
        //2 bombers get hit
        assertEquals(5,germany.getUnits().getMatches(Matches.unitIsOwnedBy(british)).size());
    }
    
    public void testStratBombRaidWithHeavyBombers()
    {
        Territory germany = m_data.getMap().getTerritory("Germany");
        Territory uk = m_data.getMap().getTerritory("United Kingdom");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        
        
        BattleTracker tracker = new BattleTracker();
        StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, germans,  tracker );
        
        battle.addAttackChange(m_data.getMap().getRoute(uk, germany), uk.getUnits().getMatches(Matches.UnitIsStrategicBomber));

        ITestDelegateBridge bridge = getDelegateBridge(british);
        TechTracker.addAdvance(british, m_data, bridge, TechAdvance.HEAVY_BOMBER);
        
        //aa guns rolls 3, misses, bomber rolls 2 dice at 3
        bridge.setRandomSource(new ScriptedRandomSource( new int[] {3,2,2} ));

        
        //if we try to move aa, then the game will ask us if we want to move
        //fail if we are called
        InvocationHandler handler = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                return null;
            }
        };
        
        ITripleaPlayer player = (ITripleaPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {ITripleaPlayer.class}, handler ); 
        bridge.setRemote(player);
        
        
        //int PUsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        int pusBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        
        battle.fight(bridge);
        
        //int PUsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        int pusAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
        assertEquals(pusBeforeRaid - 6, pusAfterRaid);
    }
    
    
    public void testLandBattleNoSneakAttack() 
    {
        String defender = "Germans";
        String attacker = "British";
        
        Territory attacked = territory("Libya", m_data);
        Territory from = territory("Anglo Egypt", m_data);
        
       
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        
        move(from.getUnits().getUnits(), new Route(from,attacked));
       
        moveDelegate(m_data).end();
        
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
        
        List<String> steps = battle.determineStepStrings(true, bridge);
        assertEquals(                
            Arrays.asList(
               
               
                attacker + FIRE,
                defender + SELECT_CASUALTIES,
                
                defender + FIRE,
                attacker + SELECT_CASUALTIES,
                
                REMOVE_CASUALTIES,
                
                attacker + ATTACKER_WITHDRAW
                
            ).toString(),
            steps.toString()
        );
     }
    
    
    public void testSeaBattleNoSneakAttack() 
    {
        String defender = "Germans";
        String attacker = "British";
        
        Territory attacked = territory("31 Sea Zone", m_data);
        Territory from = territory("32 Sea Zone", m_data);
        
        //1 destroyer attacks 1 destroyer
        addTo(from, destroyer(m_data).create(1,british(m_data)));
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
               
               
                attacker + FIRE,
                defender + SELECT_CASUALTIES,
                
                defender + FIRE,
                attacker + SELECT_CASUALTIES,
                
                REMOVE_CASUALTIES,
                
                attacker + ATTACKER_WITHDRAW
                
            ).toString(),
            steps.toString()
        );
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
               
               
                attacker + SUBS_FIRE,
                defender + SELECT_SUB_CASUALTIES,
                
                defender + SUBS_FIRE,
                attacker + SELECT_SUB_CASUALTIES,
                
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                REMOVE_CASUALTIES,
                
                attacker + SUBS_SUBMERGE,
                defender + SUBS_SUBMERGE,
                attacker + ATTACKER_WITHDRAW
                
            ).toString(),
            steps.toString()
        );
        
        List<IExecutable> execs = battle.getBattleExecutables();
        int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
        int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
        
        assertTrue(attackSubs < defendSubs);
        
        
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
        
        //2 sub attacks 1 sub and 1 destroyer
        addTo(from, submarine(m_data).create(2,british(m_data)));
        addTo(attacked, submarine(m_data).create(1,germans(m_data)));
        addTo(attacked, destroyer(m_data).create(1,germans(m_data)));
        
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        
        move(from.getUnits().getUnits(), new Route(from,attacked));
       
        moveDelegate(m_data).end();
        
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
        
        List<String> steps = battle.determineStepStrings(true, bridge);
        
        /* Here are the exact errata clarifications on how REVISED rules subs work:
         * 
         * Every sub, regardless of whether it’s on the attacking or defending side, fires in the Opening Fire step of combat. That’s the only time a sub ever fires.
         * Losses caused by attacking or defending subs are removed at the end of the Opening Fire step, before normal attack and defense rolls, unless the enemy has a destroyer present.
         * If the enemy (attacker or defender) has a destroyer, then hits caused by your subs aren’t removed until the Remove Casualties step (step 6) of combat.
         * 
         * In other words, subs work exactly the same for the attacker and the defender. Nothing, not even a destroyer, ever stops a sub from rolling its die (attack or defense) in the Opening Fire step. 
         * What a destroyer does do is let you keep your units that were sunk by enemy subs on the battle board until step 6, allowing them to fire back before going to the scrap heap.
         */
        assertEquals(                
            Arrays.asList(
            	attacker + SUBS_FIRE,
            	defender + SELECT_SUB_CASUALTIES,
                
                defender + SUBS_FIRE,
                attacker + SELECT_SUB_CASUALTIES,
                
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                
                defender + FIRE,
                attacker + SELECT_CASUALTIES,
                
                REMOVE_CASUALTIES,
                
                defender + SUBS_SUBMERGE,
                attacker + ATTACKER_WITHDRAW
            ).toString(),
            steps.toString()
        );
        
        List<IExecutable> execs = battle.getBattleExecutables();
        int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
        int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
        
        assertTrue(attackSubs < defendSubs);
        
        bridge.setRemote(new DummyTripleAPlayer());
        
        //attacking subs fires, defending destroyer and sub still gets to fire
        //attacking subs still gets to fire even if defending sub hits
        ScriptedRandomSource randomSource = new ScriptedRandomSource(
            0,2,0,0,ScriptedRandomSource.ERROR);
        bridge.setRandomSource(randomSource);
        battle.fight(bridge);
        
        assertEquals(4, randomSource.getTotalRolled());
        assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(british(m_data))).isEmpty());
        assertEquals(1, attacked.getUnits().size());
    }
    
    public void testAttackSubsAndBBOnDestroyerAndSubs() 
    {
        String defender = "Germans";
        String attacker = "British";
        
        Territory attacked = territory("31 Sea Zone", m_data);
        Territory from = territory("32 Sea Zone", m_data);
        
        //1 sub and 1 BB (two hp) attacks 3 subs and 1 destroyer
        addTo(from, submarine(m_data).create(1,british(m_data)));
        addTo(from, battleship(m_data).create(1,british(m_data)));
        addTo(attacked, submarine(m_data).create(3,germans(m_data)));
        addTo(attacked, destroyer(m_data).create(1,germans(m_data)));
        
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        
        move(from.getUnits().getUnits(), new Route(from,attacked));
       
        moveDelegate(m_data).end();
        
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
        
        List<String> steps = battle.determineStepStrings(true, bridge);
        
        /* Here are the exact errata clarifications on how REVISED rules subs work:
         * 
         * Every sub, regardless of whether it’s on the attacking or defending side, fires in the Opening Fire step of combat. That’s the only time a sub ever fires.
         * Losses caused by attacking or defending subs are removed at the end of the Opening Fire step, before normal attack and defense rolls, unless the enemy has a destroyer present.
         * If the enemy (attacker or defender) has a destroyer, then hits caused by your subs aren’t removed until the Remove Casualties step (step 6) of combat.
         * 
         * In other words, subs work exactly the same for the attacker and the defender. Nothing, not even a destroyer, ever stops a sub from rolling its die (attack or defense) in the Opening Fire step. 
         * What a destroyer does do is let you keep your units that were sunk by enemy subs on the battle board until step 6, allowing them to fire back before going to the scrap heap.
         */
        assertEquals(                
            Arrays.asList(
            	attacker + SUBS_FIRE,
            	defender + SELECT_SUB_CASUALTIES,
                
                defender + SUBS_FIRE,
                attacker + SELECT_SUB_CASUALTIES,
                
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                
                attacker + FIRE,
                defender + SELECT_CASUALTIES,
                
                defender + FIRE,
                attacker + SELECT_CASUALTIES,
                
                REMOVE_CASUALTIES,
                
                defender + SUBS_SUBMERGE,
                attacker + ATTACKER_WITHDRAW
            ).toString(),
            steps.toString()
        );
        
        List<IExecutable> execs = battle.getBattleExecutables();
        int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
        int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
        
        assertTrue(attackSubs < defendSubs);
        
        bridge.setRemote(new DummyTripleAPlayer());
        
        //attacking subs fires, defending destroyer and sub still gets to fire
        //attacking subs still gets to fire even if defending sub hits
        //battleship will not get to fire since it is killed by defending sub's sneak attack
        ScriptedRandomSource randomSource = new ScriptedRandomSource(
            0,0,0,0,ScriptedRandomSource.ERROR);
        bridge.setRandomSource(randomSource);
        battle.fight(bridge);
        
        assertEquals(4, randomSource.getTotalRolled());
        assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(british(m_data))).isEmpty());
        assertEquals(3, attacked.getUnits().size());
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
               
                
                attacker + SUBS_FIRE,
                defender + SELECT_SUB_CASUALTIES,
                
                defender + SUBS_FIRE,
                attacker + SELECT_SUB_CASUALTIES,
                
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                
                attacker + FIRE,
                defender + SELECT_CASUALTIES,
                
                REMOVE_CASUALTIES,
                attacker + SUBS_SUBMERGE,
                attacker + ATTACKER_WITHDRAW
                
            ).toString(),
            steps.toString()
        );
        
        List<IExecutable> execs = battle.getBattleExecutables();
        int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
        int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
        
        assertTrue(attackSubs < defendSubs);
        
        bridge.setRemote(new DummyTripleAPlayer());
        
        //attacking sub hits with sneak attack, but defending sub gets to return fire because it is a sub and this is revised rules
        ScriptedRandomSource randomSource = new ScriptedRandomSource(
            0,0,ScriptedRandomSource.ERROR);
        bridge.setRandomSource(randomSource);
        battle.fight(bridge);
        
        assertEquals(2, randomSource.getTotalRolled());
        assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(germans(m_data))).isEmpty());
        assertEquals(1, attacked.getUnits().size());
    }
    
    public void testAttackSubsAndDestroyerOnBBAndSubs() 
    {
        String defender = "Germans";
        String attacker = "British";
        
        Territory attacked = territory("31 Sea Zone", m_data);
        Territory from = territory("32 Sea Zone", m_data);
        
        //1 sub and 1 BB (two hp) attacks 3 subs and 1 destroyer
        addTo(from, submarine(m_data).create(3,british(m_data)));
        addTo(from, destroyer(m_data).create(1,british(m_data)));
        addTo(attacked, submarine(m_data).create(1,germans(m_data)));
        addTo(attacked, battleship(m_data).create(1,germans(m_data)));
        
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        
        move(from.getUnits().getUnits(), new Route(from,attacked));
       
        moveDelegate(m_data).end();
        
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
        
        List<String> steps = battle.determineStepStrings(true, bridge);
        
        /* Here are the exact errata clarifications on how REVISED rules subs work:
         * 
         * Every sub, regardless of whether it’s on the attacking or defending side, fires in the Opening Fire step of combat. That’s the only time a sub ever fires.
         * Losses caused by attacking or defending subs are removed at the end of the Opening Fire step, before normal attack and defense rolls, unless the enemy has a destroyer present.
         * If the enemy (attacker or defender) has a destroyer, then hits caused by your subs aren’t removed until the Remove Casualties step (step 6) of combat.
         * 
         * In other words, subs work exactly the same for the attacker and the defender. Nothing, not even a destroyer, ever stops a sub from rolling its die (attack or defense) in the Opening Fire step. 
         * What a destroyer does do is let you keep your units that were sunk by enemy subs on the battle board until step 6, allowing them to fire back before going to the scrap heap.
         */
        assertEquals(                
            Arrays.asList(
            	attacker + SUBS_FIRE,
            	defender + SELECT_SUB_CASUALTIES,
                
                defender + SUBS_FIRE,
                attacker + SELECT_SUB_CASUALTIES,
                
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                
                attacker + FIRE,
                defender + SELECT_CASUALTIES,
                
                defender + FIRE,
                attacker + SELECT_CASUALTIES,
                
                REMOVE_CASUALTIES,
                
                attacker + SUBS_SUBMERGE,
                attacker + ATTACKER_WITHDRAW
            ).toString(),
            steps.toString()
        );
        
        List<IExecutable> execs = battle.getBattleExecutables();
        int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
        int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
        
        assertTrue(attackSubs < defendSubs);
        
        bridge.setRemote(new DummyTripleAPlayer());
        
        //attacking subs fires, defending destroyer and sub still gets to fire
        //attacking subs still gets to fire even if defending sub hits
        //battleship will not get to fire since it is killed by defending sub's sneak attack
        ScriptedRandomSource randomSource = new ScriptedRandomSource(
            0,0,0,0,ScriptedRandomSource.ERROR);
        bridge.setRandomSource(randomSource);
        battle.fight(bridge);
        
        assertEquals(4, randomSource.getTotalRolled());
        assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(germans(m_data))).isEmpty());
        assertEquals(3, attacked.getUnits().size());
    }

    

    public void testAttackDestroyerAndSubsAgainstSubAndDestroyer() 
    {
        String defender = "Germans";
        String attacker = "British";
        
        Territory attacked = territory("31 Sea Zone", m_data);
        Territory from = territory("32 Sea Zone", m_data);
        
        //1 sub and 1 destroyer attack 1 sub and 1 destroyer
        //defender sneak attacks, not attacker
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
        
        List<IExecutable> execs = battle.getBattleExecutables();
        int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
        int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
        
        assertTrue(attackSubs < defendSubs);
        
        bridge.setRemote(new DummyTripleAPlayer());
        
        
        bridge.setRemote(new DummyTripleAPlayer(){

            @Override
            public CasualtyDetails selectCasualties(Collection<Unit> selectFrom,
                Map<Unit, Collection<Unit>> dependents, int count, String message,
                DiceRoll dice, PlayerID hit, CasualtyList defaultCasualties, GUID battleID) {
                
                return new CasualtyDetails(
                    Arrays.asList(selectFrom.iterator().next()),
                    Collections.<Unit>emptyList(), false);
            }
            
        });
        
 
        ScriptedRandomSource randomSource = new ScriptedRandomSource(
            0,0,0,0,ScriptedRandomSource.ERROR);
        bridge.setRandomSource(randomSource);
        battle.fight(bridge);
        
        assertEquals(4, randomSource.getTotalRolled());            
        assertEquals(0, attacked.getUnits().size());
    }

    
    public void testUnplacedDie() 
    {
        PlaceDelegate del = placeDelegate(m_data);
        del.start(getDelegateBridge(british(m_data)), m_data);
        
        addTo(british(m_data), 
              transports(m_data).create(1,british(m_data)));
        
        del.end();
        
        //unplaced units die
        assertTrue(british(m_data).getUnits().isEmpty());        
    }
    

    public void testRocketsDontFireInConquered() 
    {
        MoveDelegate move = moveDelegate(m_data);
        ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
        bridge.setStepName("CombatMove");
        bridge.setRemote(new DummyTripleAPlayer()
        {
            
        });
        move.start(bridge,m_data);
        
        //remove the russians units in caucasus so we can blitz
        Territory cauc = territory("Caucasus", m_data);
        removeFrom(cauc, cauc.getUnits().getMatches(Matches.UnitIsNotAA));
        
        //blitz
        Territory wr = territory("West Russia",m_data);
        move(wr.getUnits().getMatches(Matches.UnitCanBlitz), new Route(wr,cauc));
        
        Set<Territory> fire = new RocketsFireHelper().getTerritoriesWithRockets(m_data, germans(m_data));
        //germany, WE, SE, but not caucusus
        assertEquals(fire.size(), 3);
    }
    
    public void testTechRolls()
    {
    	//Set up the test
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");

        ITestDelegateBridge delegateBridge = getDelegateBridge(germans);
        delegateBridge.setStepName("germanTech");
        TechnologyDelegate techDelegate = techDelegate(m_data);
        techDelegate.start(delegateBridge, m_data);
    	TechAttachment ta = TechAttachment.get(germans);
    	PlayerAttachment pa = PlayerAttachment.get(germans);
    	TechnologyFrontier rockets = new TechnologyFrontier("",m_data);
    	rockets.addAdvance(TechAdvance.ROCKETS);
    	TechnologyFrontier jet = new TechnologyFrontier("",m_data);
    	jet.addAdvance(TechAdvance.JET_POWER);
    	//Check to make sure it was successful
    	int initPUs = germans.getResources().getQuantity("PUs"); 
    	
    	//Fail the roll
    	delegateBridge.setRandomSource(new ScriptedRandomSource(new int[]{ 3, 4 }));
    	TechResults roll = techDelegate.rollTech(2, rockets, 0);

    	//Check to make sure it failed
    	assertEquals(0, roll.getHits());
    	int midPUs = germans.getResources().getQuantity("PUs"); 
    	assertEquals(initPUs-10,midPUs);
    	
    	
    	//Make a Successful roll
    	delegateBridge.setRandomSource(new ScriptedRandomSource(new int[]{ 5 }));
    	TechResults roll2 = techDelegate.rollTech(1, rockets, 0);
    	
    	//Check to make sure it succeeded
    	assertEquals(1, roll2.getHits());
    	int finalPUs = germans.getResources().getQuantity("PUs"); 
    	assertEquals(midPUs-5,finalPUs);
    	
    	//Test the variable tech cost
	    ta.setTechCost("6");//Make a Successful roll
    	delegateBridge.setRandomSource(new ScriptedRandomSource(new int[]{ 5 }));
    	TechResults roll3 = techDelegate.rollTech(1, jet, 0);
    	
    	//Check to make sure it succeeded
    	assertEquals(1, roll3.getHits());
    	int VariablePUs = germans.getResources().getQuantity("PUs"); 
    	assertEquals(finalPUs-6,VariablePUs);	    
    }
    
    public void testTransportsUnloadingToMultipleTerritoriesDie() {
    	
    	//two transports enter a battle, but drop off
    	//their units to two allied territories before
    	//the begin the battle
    	//the units they drop off should die with the transports
    	
    	PlayerID germans = germans(m_data);
    	PlayerID british = british(m_data);
    	Territory sz6 =  territory("6 Sea Zone", m_data);
    	Territory sz5 =  territory("5 Sea Zone", m_data);
    	Territory germany = territory("Germany", m_data);
    	Territory norway = territory("Norway", m_data);
    	Territory we = territory("Western Europe", m_data);
    	
    	addTo(sz6, destroyer(m_data).create(2, british));
    	addTo(sz5, transports(m_data).create(1, germans));
    	
    	ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
        bridge.setStepName("CombatMove");
        bridge.setRemote(getDummyPlayer());
        moveDelegate(m_data).start(bridge, m_data);
         
        //load two transports, 1 tank each
        load(germany.getUnits().getMatches(Matches.UnitCanBlitz).subList(0, 1), new Route(germany, sz5));
        load(germany.getUnits().getMatches(Matches.UnitCanBlitz).subList(0, 1), new Route(germany, sz5));
        
        //attack sz 6
        move(sz5.getUnits().getMatches(new CompositeMatchOr<Unit>(Matches.UnitCanBlitz, Matches.UnitIsTransport)), new Route(sz5, sz6));
        
        //unload transports, 1 each to a different country
        move(sz6.getUnits().getMatches(Matches.UnitCanBlitz).subList(0, 1), new Route(sz6, norway));
        move(sz6.getUnits().getMatches(Matches.UnitCanBlitz).subList(0, 1), new Route(sz6, we));
        
        
        //fight the battle
        moveDelegate(m_data).end();
        
        
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(sz6, false);
        
        //everything hits, this will kill both transports
        bridge.setRandomSource(new ScriptedRandomSource(0));
        battle.fight(bridge);
         
        
    	//the armour should have died
        assertEquals(0, norway.getUnits().countMatches(Matches.UnitCanBlitz));
        assertEquals(2, we.getUnits().countMatches(Matches.UnitCanBlitz));
    	
    }
    
    public void testCanalMovePass() 
    {
    	
    	Territory sz15 = territory("15 Sea Zone", m_data);
    	Territory sz34 = territory("34 Sea Zone", m_data);
    	
    	
    	
    	ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
    	MoveDelegate moveDelegate = moveDelegate(m_data);
    	
		moveDelegate.start(bridge, m_data);
    	String error = moveDelegate.move(sz15.getUnits().getUnits(), new Route(sz15, sz34));
    	assertValid(error);
    	
    }
    
    public void testCanalMovementFail() 
    {
    	Territory sz14 = territory("14 Sea Zone", m_data);
    	Territory sz15 = territory("15 Sea Zone", m_data);
    	Territory sz34 = territory("34 Sea Zone", m_data);
    	
    	//clear the british in sz 15
    	removeFrom(sz15, sz15.getUnits().getUnits());
    	
    	ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
        bridge.setStepName("CombatMove");
    	MoveDelegate moveDelegate = moveDelegate(m_data);
    	
		moveDelegate.start(bridge, m_data);
    	String error = moveDelegate.move(sz14.getUnits().getUnits(), new Route(sz14, sz15, sz34));
    	assertError(error);
    	
    }
    
    /***********************************************************/
    /***********************************************************/
    /***********************************************************/
    /***********************************************************/

    private ITripleaPlayer getDummyPlayer()
    {
        return new DummyTripleAPlayer();
        
    }
    
    public void testTransportIsTransport() 
    {
        assertTrue(Matches.UnitIsTransport.match(transports(m_data).create(british(m_data))));
        assertFalse(Matches.UnitIsTransport.match(infantry(m_data).create(british(m_data))));
    }
    
}


