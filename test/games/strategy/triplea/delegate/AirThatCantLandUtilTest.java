package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.player.ITripleaPlayer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;

import junit.framework.TestCase;

public class AirThatCantLandUtilTest extends TestCase
{
    private GameData m_data;
    private PlayerID m_americans;
    
    private UnitType m_fighter;
    
    
    @Override
    protected void setUp() throws Exception
    {
        m_data = LoadGameUtil.loadGame("revised", "revised.xml");
        
        m_americans = m_data.getPlayerList().getPlayerID("Americans");
    
        m_fighter = m_data.getUnitTypeList().getUnitType("fighter");
    
        
    }

    private ITestDelegateBridge getDelegateBridge(PlayerID player)
    {
        return GameDataTestUtil.getDelegateBridge(player);
    }
    
    public void testSimple()
    {
        PlayerID player = m_americans;
        //everything can land
        ITestDelegateBridge bridge = getDelegateBridge(player);
        AirThatCantLandUtil util = new AirThatCantLandUtil(m_data, bridge);
        assertTrue(util.getTerritoriesWhereAirCantLand(player).isEmpty());
    }
    
    public void testCantLandEnemyTerritory()
    {
        PlayerID player = m_americans;
        ITestDelegateBridge bridge = getDelegateBridge(player);
        Territory balkans = m_data.getMap().getTerritory("Balkans");
        Change addAir = ChangeFactory.addUnits(balkans, m_fighter.create(2, player));
        
        new ChangePerformer(m_data).perform(addAir);
        
        AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(m_data, bridge);
        Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
        assertEquals(1,cantLand.size());
        assertEquals(balkans, cantLand.iterator().next());
        
        airThatCantLandUtil.removeAirThatCantLand(player, false);
        //jsut the original german fighter
        assertEquals(1,balkans.getUnits().getMatches(Matches.UnitIsAir).size());
    }
    

    
    public void testCantLandWater()
    {
        PlayerID player = m_americans;
        ITestDelegateBridge bridge = getDelegateBridge(player);
        Territory sz_55 = m_data.getMap().getTerritory("55 Sea Zone");
        Change addAir = ChangeFactory.addUnits(sz_55, m_fighter.create(2, player));
        
        new ChangePerformer(m_data).perform(addAir);
        
        AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(m_data, bridge);
        Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
        assertEquals(1,cantLand.size());
        assertEquals(sz_55, cantLand.iterator().next());
        
        airThatCantLandUtil.removeAirThatCantLand(player, false);
        assertEquals(0,sz_55.getUnits().getMatches(Matches.UnitIsAir).size());

    }

    
    public void testSpareNextToFactory()
    {
        PlayerID player = m_americans;
        ITestDelegateBridge bridge = getDelegateBridge(player);
        Territory sz_55 = m_data.getMap().getTerritory("55 Sea Zone");
        Change addAir = ChangeFactory.addUnits(sz_55, m_fighter.create(2, player));
        
        new ChangePerformer(m_data).perform(addAir);
        
        AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(m_data, bridge);
                
        airThatCantLandUtil.removeAirThatCantLand(player, true);
        assertEquals(2,sz_55.getUnits().getMatches(Matches.UnitIsAir).size());

    }
    
    
    public void testCantLandCarrier()
    {
        //1 carrier in the region, but three fighters, make sure we cant land
        
        PlayerID player = m_americans;
        ITestDelegateBridge bridge = getDelegateBridge(player);
        Territory sz_52 = m_data.getMap().getTerritory("52 Sea Zone");
        Change addAir = ChangeFactory.addUnits(sz_52, m_fighter.create(2, player));
        
        new ChangePerformer(m_data).perform(addAir);
        
        AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(m_data, bridge);
        Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
        assertEquals(1,cantLand.size());
        assertEquals(sz_52, cantLand.iterator().next());
        
        airThatCantLandUtil.removeAirThatCantLand(player, false);
        //just the original american fighter, plus one that can land on the carrier
        assertEquals(2,sz_52.getUnits().getMatches(Matches.UnitIsAir).size());
    }

    public void testCanLandNeighborCarrier()
    {    	
    	PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
        
        ITestDelegateBridge bridge = getDelegateBridge(japanese);

        //we need to initialize the original owner
        InitializationDelegate initDel = (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
        initDel.start(bridge, m_data);
        initDel.end();
        
        //Get necessary sea zones and unit types for this test
        Territory sz_44 = m_data.getMap().getTerritory("44 Sea Zone");
        Territory sz_45 = m_data.getMap().getTerritory("45 Sea Zone");
        Territory sz_52 = m_data.getMap().getTerritory("52 Sea Zone");

        UnitType subType = m_data.getUnitTypeList().getUnitType("submarine");
        UnitType carrierType = m_data.getUnitTypeList().getUnitType("carrier");
        UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
        
        //Add units for the test
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_45, subType.create(1, japanese)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_44, carrierType.create(1, americans)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_44, fighterType.create(1, americans)));
        
        //Get total number of defending units before the battle 
        Integer preCountSz_52 = sz_52.getUnits().size();
        Integer preCountAirSz_44 = sz_44.getUnits().getMatches(Matches.UnitIsAir).size(); 

        //now move to attack
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");        
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);
        moveDelegate.move(sz_45.getUnits().getUnits(), m_data.getMap().getRoute(sz_45, sz_44));        
        moveDelegate.end();
                
        //fight the battle
        BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
        battle.start(bridge, m_data);
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {0,0,0}));
        bridge.setRemote(getDummyPlayer());
        battle.fightBattle(sz_44, false);        
        battle.end();
        
        //Get the total number of units that should be left after the planes retreat
        Integer expectedCountSz_52 = sz_52.getUnits().size(); 
        Integer postCountInt = preCountSz_52 + preCountAirSz_44;    

        //Compare the expected count with the actual number of units in landing zone
        assertEquals(expectedCountSz_52, postCountInt);    	
    }
    
    public void testCanLandMultiNeighborCarriers()
    {
    	PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
        
        ITestDelegateBridge bridge = getDelegateBridge(japanese);

        //we need to initialize the original owner
        InitializationDelegate initDel = (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
        initDel.start(bridge, m_data);
        initDel.end();
        
        //Get necessary sea zones and unit types for this test
        Territory sz_43 = m_data.getMap().getTerritory("43 Sea Zone");
        Territory sz_44 = m_data.getMap().getTerritory("44 Sea Zone");
        Territory sz_45 = m_data.getMap().getTerritory("45 Sea Zone");
        Territory sz_52 = m_data.getMap().getTerritory("52 Sea Zone");

        UnitType subType = m_data.getUnitTypeList().getUnitType("submarine");
        UnitType carrierType = m_data.getUnitTypeList().getUnitType("carrier");
        UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
        
        //Add units for the test
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_45, subType.create(1, japanese)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_44, carrierType.create(1, americans)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_44, fighterType.create(3, americans)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_43, carrierType.create(1, americans)));
        
        //Get total number of defending units before the battle 
        Integer preCountSz_52 = sz_52.getUnits().size(); 
        Integer preCountSz_43 = sz_43.getUnits().size(); 

        //now move to attack
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");        
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);
        moveDelegate.move(sz_45.getUnits().getUnits(), m_data.getMap().getRoute(sz_45, sz_44));        
        moveDelegate.end();
                
        //fight the battle
        BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
        battle.start(bridge, m_data);
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {0,0,0}));
        bridge.setRemote(getDummyPlayer());
        battle.fightBattle(sz_44, false);        
        battle.end();
        
        //Get the total number of units that should be left after the planes retreat
        Integer expectedCountSz_52 = sz_52.getUnits().size(); 
        Integer expectedCountSz_43 = sz_43.getUnits().size(); 
        Integer postCountSz_52 = preCountSz_52 + 1;    
        Integer postCountSz_43 = preCountSz_43 + 2;    

        //Compare the expected count with the actual number of units in landing zone
        assertEquals(expectedCountSz_52, postCountSz_52);    	
        assertEquals(expectedCountSz_43, postCountSz_43);    	
    }

    public void testCanLandNeighborLand4thEd()
    {
    	PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
        
        ITestDelegateBridge bridge = getDelegateBridge(japanese);

        //we need to initialize the original owner
        InitializationDelegate initDel = (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
        initDel.start(bridge, m_data);
        initDel.end();
        
        //Get necessary sea zones and unit types for this test
        Territory sz_9 = m_data.getMap().getTerritory("9 Sea Zone");
        Territory eastCanada = m_data.getMap().getTerritory("Eastern Canada");
        Territory sz_11 = m_data.getMap().getTerritory("11 Sea Zone");

        UnitType subType = m_data.getUnitTypeList().getUnitType("submarine");
        UnitType carrierType = m_data.getUnitTypeList().getUnitType("carrier");
        UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
        
        //Add units for the test
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_11, subType.create(1, japanese)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_9, carrierType.create(1, americans)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_9, fighterType.create(1, americans)));
        
        //Get total number of defending units before the battle 
        Integer preCountCanada = eastCanada.getUnits().size();
        Integer preCountAirSz_9 = sz_9.getUnits().getMatches(Matches.UnitIsAir).size(); 

        //now move to attack
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");        
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);
        moveDelegate.move(sz_11.getUnits().getUnits(), m_data.getMap().getRoute(sz_11, sz_9));        
        moveDelegate.end();
                
        //fight the battle
        BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
        battle.start(bridge, m_data);
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {0,}));
        bridge.setRemote(getDummyPlayer());
        battle.fightBattle(sz_9, false);        
        battle.end();
        
        //Get the total number of units that should be left after the planes retreat
        Integer expectedCountCanada = eastCanada.getUnits().size(); 
        Integer postCountInt = preCountCanada + preCountAirSz_9;    

        //Compare the expected count with the actual number of units in landing zone
        assertEquals(expectedCountCanada, postCountInt);      
    }

    public void testCanLandNeighborLandWithRetreatedBattle4thEd()
    {
    	PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
        
        ITestDelegateBridge bridge = getDelegateBridge(japanese);

        //we need to initialize the original owner
        InitializationDelegate initDel = (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
        initDel.start(bridge, m_data);
        initDel.end();
        
        //Get necessary sea zones and unit types for this test
        Territory sz_9 = m_data.getMap().getTerritory("9 Sea Zone");
        Territory eastCanada = m_data.getMap().getTerritory("Eastern Canada");
        Territory sz_11 = m_data.getMap().getTerritory("11 Sea Zone");

        UnitType subType = m_data.getUnitTypeList().getUnitType("submarine");
        UnitType carrierType = m_data.getUnitTypeList().getUnitType("carrier");
        UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
        UnitType transportType = m_data.getUnitTypeList().getUnitType("transport");
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        
        //Add units for the test
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_11, subType.create(1, japanese)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_11, transportType.create(1, japanese)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_11, infantryType.create(1, japanese)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_9, carrierType.create(1, americans)));
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sz_9, fighterType.create(2, americans)));
        
        //Get total number of defending units before the battle 
        Integer preCountCanada = eastCanada.getUnits().size();
        Integer preCountAirSz_9 = sz_9.getUnits().getMatches(Matches.UnitIsAir).size(); 

        //now move to attack
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");        
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);
        moveDelegate.move(sz_11.getUnits().getUnits(), m_data.getMap().getRoute(sz_11, sz_9));   
        moveDelegate.move(sz_9.getUnits().getUnits(infantryType, 1), m_data.getMap().getRoute(sz_9, eastCanada));      
        moveDelegate.end();
                
        //fight the battle
        BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
        battle.start(bridge, m_data);
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {0,0,0}));
        bridge.setRemote(getDummyPlayer());
        battle.fightBattle(sz_9, false);        
        battle.end();
        
        //Get the total number of units that should be left after the planes retreat
        Integer expectedCountCanada = eastCanada.getUnits().size(); 
        Integer postCountInt = preCountCanada + preCountAirSz_9;    

        //Compare the expected count with the actual number of units in landing zone
        assertEquals(expectedCountCanada, postCountInt);  
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
