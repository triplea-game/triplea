package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.assertError;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.getDelegateBridge;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.LoadGameUtil;

import java.io.File;

import junit.framework.TestCase;

public class BigWorldTest extends TestCase
{
	private GameData m_data;
	
	@Override
	protected void setUp() throws Exception
	{
		m_data = LoadGameUtil.loadGame("Big World : 1942", "big_world" + File.separator + "games" + File.separator + "big_world_1942.xml");
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		m_data = null;
	}
	
	public void testCanalMovementNotStartingInCanalZone()
	{
		final Territory sz28 = territory("SZ 28 Eastern Mediterranean", m_data);
		final Territory sz27 = territory("SZ 27 Aegean Sea", m_data);
		final Territory sz29 = territory("SZ 29 Black Sea", m_data);
		final ITestDelegateBridge bridge = getDelegateBridge(british(m_data), m_data);
		bridge.setStepName("CombatMove");
		final MoveDelegate moveDelegate = moveDelegate(m_data);
		moveDelegate.setDelegateBridgeAndPlayer(bridge);
		moveDelegate.start();
		final String error = moveDelegate.move(sz28.getUnits().getUnits(), new Route(sz28, sz27, sz29));
		assertError(error);
	}
}
