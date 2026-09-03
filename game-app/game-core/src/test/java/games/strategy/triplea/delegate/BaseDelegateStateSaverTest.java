package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/** Oracle-gated round-trip test for {@link BaseDelegateStateSaver} — the root delegate state. */
class BaseDelegateStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private BaseDelegateState sampleState() {
    final BaseDelegateState state = new BaseDelegateState();
    state.startBaseStepsFinished = true;
    state.endBaseStepsFinished = true;
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new BaseDelegateStateSaver(), sampleState(), gameData);
  }
}
