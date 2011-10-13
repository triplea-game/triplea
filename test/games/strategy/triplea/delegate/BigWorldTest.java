package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.*;

import java.io.File;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.LoadGameUtil;
import junit.framework.TestCase;

public class BigWorldTest extends TestCase {
	
    private GameData m_data;

    @Override
    protected void setUp() throws Exception
    {
        m_data = LoadGameUtil.loadGame("Big World : 1942", "big_world" + File.separator + "games" +  File.separator + "big_world_1942.xml");
    }

    @Override
    protected void tearDown() throws Exception
    {
        m_data = null;
    }
    
    public void testCanalMovementNotStartingInCanalZone() 
    {
    	Territory sz28 = territory("SZ 28 Eastern Mediterranean", m_data);
    	Territory sz27 = territory("SZ 27 Aegean Sea", m_data);
    	Territory sz29 = territory("SZ 29 Black Sea", m_data);
    	
    	ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
    	MoveDelegate moveDelegate = moveDelegate(m_data);
    	
        moveDelegate.start(bridge);
    	String error = moveDelegate.move(sz28.getUnits().getUnits(), new Route(sz28, sz27, sz29));
    	assertError(error);
    	
    }

}
