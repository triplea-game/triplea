package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/** Oracle-gated round-trip test for {@link EndTurnStateSaver}. */
class EndTurnStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private EndTurnExtendedDelegateState sampleState() {
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    base.endBaseStepsFinished = false;

    final EndTurnExtendedDelegateState state = new EndTurnExtendedDelegateState();
    state.superState = base;
    state.needToInitialize = true;
    state.hasPostedTurnSummary = true;
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new EndTurnStateSaver(), sampleState(), gameData);
  }
}
