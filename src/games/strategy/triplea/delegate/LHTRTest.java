package games.strategy.triplea.delegate;

import java.io.*;
import java.lang.reflect.*;
import java.util.List;

import games.strategy.engine.data.*;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.random.IRandomSource;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.player.ITripleaPlayer;
import junit.framework.TestCase;

public class LHTRTest extends TestCase
{
    
    private GameData m_data;

    @Override
    protected void setUp() throws Exception
    {
        File gameRoot  = GameRunner.getRootFolder();
        File gamesFolder = new File(gameRoot, "games");
        File lhtr = new File(gamesFolder, "lhtr_incomplete.xml");
        
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
        
        TestDelegateBridge bridge = new TestDelegateBridge( m_data, germans  );
                
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
        
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, japanese);
        
        TechTracker.addAdvance(japanese, m_data, bridge, TechAdvance.SUPER_SUBS);
        
        
        //after tech advance, this is now 3
        assertEquals(3, attachment.getDefense(japanese));
        assertEquals(3, attachment.getAttack(japanese));

        //make sure this only changes for the player with the tech
        PlayerID americans = m_data.getPlayerList().getPlayerID("Americans"); 
        assertEquals(2, attachment.getDefense(americans));
        assertEquals(2, attachment.getAttack(americans));
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