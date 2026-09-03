package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class SpecialMoveStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private SpecialMoveExtendedDelegateState sampleState() {
    final SpecialMoveExtendedDelegateState s = new SpecialMoveExtendedDelegateState();
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    base.endBaseStepsFinished = false;
    s.superState = base;
    s.needToInitialize = true;
    return s;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new SpecialMoveStateSaver(), sampleState(), gameData);
  }
}
