package games.strategy.triplea.delegate;

import java.io.*;
import java.lang.reflect.*;

import junit.framework.TestCase;

import games.strategy.engine.data.*;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.player.ITripleaPlayer;

public class RevisedTest extends TestCase 
{

    private GameData m_data;

    @Override
    protected void setUp() throws Exception
    {
        File gameRoot  = GameRunner.getRootFolder();
        File gamesFolder = new File(gameRoot, "games");
        File lhtr = new File(gamesFolder, "revised.xml");
        
        if(!lhtr.exists())
            throw new IllegalStateException("revised does not exist");
        
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
    
    public void testSubAdvance()
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
        assertEquals(2, attachment.getDefense(japanese));
        assertEquals(3, attachment.getAttack(japanese));
    }
    
    public void testRetreatBug()
    {
        
        PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
        PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
        
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, russians);

        //we need to initialize the original owner
        InitializationDelegate initDel = (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
        initDel.start(bridge, m_data);
        initDel.end();

        
        //make sinkian japanese owned, put one infantry in it
        Territory sinkiang = m_data.getMap().getTerritory("Sinkiang");
        sinkiang.getUnits().removeAllUnits(sinkiang.getUnits().getUnits());
        PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        sinkiang.setOwner(japanese);

        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        sinkiang.getUnits().addAllUnits(infantryType.create(1, japanese));
        
        
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

    
    

    private ITripleaPlayer getDummyPlayer()
    {
        InvocationHandler handler = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                return null;
            }
        };
        
        return (ITripleaPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {ITripleaPlayer.class}, handler ); 
        
        
    }
    
}


