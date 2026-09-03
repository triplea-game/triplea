package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/** Oracle-gated round-trip test for {@link TechActivationStateSaver}. */
class TechActivationStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private TechActivationExtendedDelegateState sampleState() {
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    base.endBaseStepsFinished = false;

    final TechActivationExtendedDelegateState state = new TechActivationExtendedDelegateState();
    state.superState = base;
    state.needToInitialize = true;
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new TechActivationStateSaver(), sampleState(), gameData);
  }
}
