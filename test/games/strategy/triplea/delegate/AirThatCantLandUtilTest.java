package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TestDelegateBridge;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.GameRunner;
import games.strategy.triplea.ui.display.DummyDisplay;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
        File gameRoot  = GameRunner.getRootFolder();
        File gamesFolder = new File(gameRoot, "games");
        File lhtr = new File(gamesFolder, "revised.xml");
        
        if(!lhtr.exists())
            throw new IllegalStateException("revised does not exist");
        
        InputStream input = new BufferedInputStream(new FileInputStream(lhtr));
        
        try
        {
            m_data = (new GameParser()).parse(input);
            m_americans = m_data.getPlayerList().getPlayerID("Americans");
            m_fighter = m_data.getUnitTypeList().getUnitType("fighter");
        }
        finally
        {
            input.close();    
        }
    }
    
    public void testSimple()
    {
        //everything can land
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, m_americans, (IDisplay) new DummyDisplay());
        AirThatCantLandUtil util = new AirThatCantLandUtil(m_data, bridge);
        assertTrue(util.getTerritoriesWhereAirCantLand().isEmpty());
    }
    
    public void testCantLandEnemyTerritory()
    {
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, m_americans, (IDisplay) new DummyDisplay());
        Territory balkans = m_data.getMap().getTerritory("Balkans");
        Change addAir = ChangeFactory.addUnits(balkans, m_fighter.create(2, m_americans));
        
        new ChangePerformer(m_data).perform(addAir);
        
        AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(m_data, bridge);
        Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand();
        assertEquals(1,cantLand.size());
        assertEquals(balkans, cantLand.iterator().next());
        
        airThatCantLandUtil.removeAirThatCantLand(false);
        //jsut the original german fighter
        assertEquals(1,balkans.getUnits().getMatches(Matches.UnitIsAir).size());
    }
    

    
    public void testCantLandWater()
    {
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, m_americans, (IDisplay) new DummyDisplay());
        Territory sz_55 = m_data.getMap().getTerritory("55 Sea Zone");
        Change addAir = ChangeFactory.addUnits(sz_55, m_fighter.create(2, m_americans));
        
        new ChangePerformer(m_data).perform(addAir);
        
        AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(m_data, bridge);
        Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand();
        assertEquals(1,cantLand.size());
        assertEquals(sz_55, cantLand.iterator().next());
        
        airThatCantLandUtil.removeAirThatCantLand(false);
        assertEquals(0,sz_55.getUnits().getMatches(Matches.UnitIsAir).size());

    }

    
    public void testSpareNextToFactory()
    {
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, m_americans, (IDisplay) new DummyDisplay());
        Territory sz_55 = m_data.getMap().getTerritory("55 Sea Zone");
        Change addAir = ChangeFactory.addUnits(sz_55, m_fighter.create(2, m_americans));
        
        new ChangePerformer(m_data).perform(addAir);
        
        AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(m_data, bridge);
                
        airThatCantLandUtil.removeAirThatCantLand(true);
        assertEquals(2,sz_55.getUnits().getMatches(Matches.UnitIsAir).size());

    }
    
    
    public void testCantLandCarrier()
    {
        //1 carrier in the region, but three fighters, make sure we cant land
        
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, m_americans, (IDisplay) new DummyDisplay());
        Territory sz_52 = m_data.getMap().getTerritory("52 Sea Zone");
        Change addAir = ChangeFactory.addUnits(sz_52, m_fighter.create(2, m_americans));
        
        new ChangePerformer(m_data).perform(addAir);
        
        AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(m_data, bridge);
        Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand();
        assertEquals(1,cantLand.size());
        assertEquals(sz_52, cantLand.iterator().next());
        
        airThatCantLandUtil.removeAirThatCantLand(false);
        //just the original american fighter, plus one that can land on the carrier
        assertEquals(2,sz_52.getUnits().getMatches(Matches.UnitIsAir).size());

        
        
    }

}
