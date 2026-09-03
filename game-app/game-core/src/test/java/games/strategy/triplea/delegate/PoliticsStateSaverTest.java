package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/** Oracle-gated round-trip test for {@link PoliticsStateSaver}. */
class PoliticsStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private PoliticsExtendedDelegateState sampleState() {
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    base.endBaseStepsFinished = true;

    final PoliticsExtendedDelegateState state = new PoliticsExtendedDelegateState();
    state.superState = base;
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new PoliticsStateSaver(), sampleState(), gameData);
  }
}
