package games.strategy.triplea.delegate;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.random.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.display.DummyDisplay;
import junit.framework.TestCase;

public class LHTRTest extends TestCase
{
    
    private GameData m_data;

    @Override
    protected void setUp() throws Exception
    {
        File gameRoot  = GameRunner.getRootFolder();
        File gamesFolder = new File(gameRoot, "games");
        File lhtr = new File(gamesFolder, "lhtr.xml");
        
        if(!lhtr.exists())
            throw new IllegalStateException("LHTR does not exist");
        
        InputStream input = new BufferedInputStream(new FileInputStream(lhtr));
        
        try
        {
            m_data = (new GameParser()).parse(input);
        }
        finally
        {
            input.close();    
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        m_data = null;
    }

    public void testAAGunsDontFireNonCombat()
    {
        MoveDelegate delegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        delegate.initialize("MoveDelegate", "MoveDelegate");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        
        TestDelegateBridge bridge = new TestDelegateBridge( m_data, germans, (IDisplay) new DummyDisplay());
                
        bridge.setStepName("GermansNonCombatMove");
        delegate.start(bridge, m_data);

        //if we try to move aa, then the game will ask us if we want to move
        //fail if we are called
        InvocationHandler handler = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                fail("method called:" + method);
                //never reached
                return null;
            }
        };
        
        ITripleaPlayer player = (ITripleaPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {ITripleaPlayer.class}, handler ); 
        bridge.setRemote(player);
        
        //move 1 fighter over the aa gun in caucus
        
        Route route = new Route();
        route.setStart(m_data.getMap().getTerritory("Ukraine S.S.R."));
        route.add(m_data.getMap().getTerritory("Caucasus"));
        route.add(m_data.getMap().getTerritory("West Russia"));
        
        
        List<Unit> fighter = route.getStart().getUnits().getMatches(Matches.UnitIsAir);
        
        delegate.move(fighter, route);
    }
    
    public void testSubDefenseBonus()
    {
        UnitType sub = m_data.getUnitTypeList().getUnitType("submarine");
        UnitAttachment attachment = UnitAttachment.get(sub);
        
        PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        
        //before the advance, subs defend and attack at 2
        assertEquals(2, attachment.getDefense(japanese));
        assertEquals(2, attachment.getAttack(japanese));
        
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, japanese, (IDisplay) new DummyDisplay());
        
        TechTracker.addAdvance(japanese, m_data, bridge, TechAdvance.SUPER_SUBS);
        
        
        //after tech advance, this is now 3
        assertEquals(3, attachment.getDefense(japanese));
        assertEquals(3, attachment.getAttack(japanese));

        //make sure this only changes for the player with the tech
        PlayerID americans = m_data.getPlayerList().getPlayerID("Americans"); 
        assertEquals(2, attachment.getDefense(americans));
        assertEquals(2, attachment.getAttack(americans));
    }
    
    
    public void testLHTRBombingRaid()
    {
        Territory germany = m_data.getMap().getTerritory("Germany");
        Territory uk = m_data.getMap().getTerritory("United Kingdom");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        
        
        BattleTracker tracker = new BattleTracker();
        StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, germans,  tracker );
        
        battle.addAttack(m_data.getMap().getRoute(uk, germany), uk.getUnits().getMatches(Matches.UnitIsStrategicBomber));

        TestDelegateBridge bridge = new TestDelegateBridge(m_data, british, (IDisplay) new DummyDisplay());
        TechTracker.addAdvance(british, m_data, bridge, TechAdvance.HEAVY_BOMBER);
        
        //aa guns rolls 3, misses, bomber rolls 2 dice at 3 and 4
        bridge.setRandomSource(new ScriptedRandomSource( new int[] {3,2,3} ));

        
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
        
        
        int ipcsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.IPCS));
        
        battle.fight(bridge);
        
        int ipcsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.IPCS));
        
        //largets dice is 4, so damage is 1 + 4 = 5
        assertEquals(ipcsBeforeRaid - 5, ipcsAfterRaid);
        
        
    }
    
    
    public void testLHTRBombingRaid2Bombers()
    {
        Territory germany = m_data.getMap().getTerritory("Germany");
        Territory uk = m_data.getMap().getTerritory("United Kingdom");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        
        //add a unit
        Unit bomber = m_data.getUnitTypeList().getUnitType("bomber").create(british);
        Change change = ChangeFactory.addUnits(uk, Collections.singleton(bomber));
        new ChangePerformer(m_data).perform(change);
        
        
        BattleTracker tracker = new BattleTracker();
        StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, germans,  tracker );
        
        battle.addAttack(m_data.getMap().getRoute(uk, germany), uk.getUnits().getMatches(Matches.UnitIsStrategicBomber));

        TestDelegateBridge bridge = new TestDelegateBridge(m_data, british, (IDisplay) new DummyDisplay());
        TechTracker.addAdvance(british, m_data, bridge, TechAdvance.HEAVY_BOMBER);
        
        //aa guns rolls 3,3 both miss, bomber 1 rolls 2 dice at 3,4 and bomber 2 rolls dice at 1,2
        bridge.setRandomSource(new ScriptedRandomSource( new int[] {3,3,2,3,0,1} ));

        
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
        
        
        int ipcsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.IPCS));
        
        battle.fight(bridge);
        
        int ipcsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.IPCS));
        
        //largets dice is 4, so damage is 1 + 4 = 5
        //bomber 2 hits at 2, so damage is 3 
        assertEquals(ipcsBeforeRaid - 8, ipcsAfterRaid);
        
        
    }
    
}






/**
 * a random source that throws when asked for random
 * usefule for testing 
 */
class ThrowingRandomSource implements IRandomSource
{

    public int getRandom(int max, String annotation)
    {
        throw new IllegalStateException("not allowed");
    }

    public int[] getRandom(int max, int count, String annotation)
    {
        throw new IllegalStateException("not allowed");
    }
    
}