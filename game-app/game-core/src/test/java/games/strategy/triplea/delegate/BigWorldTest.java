package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.assertError;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BigWorldTest {
  private final GameData gameData = TestMapGameData.BIG_WORLD_1942.getGameData();

  @Test
  void testCanalMovementNotStartingInCanalZone() {
    final Territory sz28 = territory("SZ 28 Eastern Mediterranean", gameData);
    final Territory sz27 = territory("SZ 27 Aegean Sea", gameData);
    final Territory sz29 = territory("SZ 29 Black Sea", gameData);
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Optional<String> error = moveDelegate.move(sz28.getUnits(), new Route(sz28, sz27, sz29));
    assertError(error);
  }
}
