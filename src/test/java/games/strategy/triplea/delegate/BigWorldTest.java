package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.assertError;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.getDelegateBridge;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.TestMapGameData;

public class BigWorldTest {
  private GameData gameData;

  @Before
  public void setUp() throws Exception {
    gameData = TestMapGameData.BIG_WORLD_1942.getGameData();
  }

  @Test
  public void testCanalMovementNotStartingInCanalZone() {
    final Territory sz28 = territory("SZ 28 Eastern Mediterranean", gameData);
    final Territory sz27 = territory("SZ 27 Aegean Sea", gameData);
    final Territory sz29 = territory("SZ 29 Black Sea", gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData), gameData);
    bridge.setStepName("CombatMove");
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final String error = moveDelegate.move(sz28.getUnits().getUnits(), new Route(sz28, sz27, sz29));
    assertError(error);
  }
}
