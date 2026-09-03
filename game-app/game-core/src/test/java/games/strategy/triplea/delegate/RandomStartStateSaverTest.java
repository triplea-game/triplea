package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/** Oracle-gated round-trip test for {@link RandomStartStateSaver}. */
class RandomStartStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private RandomStartExtendedDelegateState sampleState() {
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    base.endBaseStepsFinished = true;

    final RandomStartExtendedDelegateState state = new RandomStartExtendedDelegateState();
    state.superState = base;
    state.currentPickingPlayer = gameData.getPlayerList().getPlayers().get(0);
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new RandomStartStateSaver(), sampleState(), gameData);
  }
}
