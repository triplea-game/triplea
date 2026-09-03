package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class MoveStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private MoveExtendedDelegateState sampleState() {
    final MoveExtendedDelegateState s = new MoveExtendedDelegateState();
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    base.endBaseStepsFinished = false;
    s.superState = base;
    s.needToInitialize = true;
    s.needToDoRockets = true;
    final IntegerMap<Territory> pusLost = new IntegerMap<>();
    pusLost.put(gameData.getMap().getTerritories().get(0), 4);
    s.pusLost = pusLost;
    return s;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new MoveStateSaver(), sampleState(), gameData);
  }
}
