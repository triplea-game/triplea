package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/** Oracle-gated round-trip test for {@link InitializationStateSaver}. */
class InitializationStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private InitializationExtendedDelegateState sampleState() {
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    base.endBaseStepsFinished = false;

    final InitializationExtendedDelegateState state = new InitializationExtendedDelegateState();
    state.superState = base;
    state.needToInitialize = true;
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new InitializationStateSaver(), sampleState(), gameData);
  }
}
