package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/** Oracle-gated round-trip test for {@link EndRoundStateSaver}. */
class EndRoundStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private EndRoundExtendedDelegateState sampleState() {
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;

    final EndRoundExtendedDelegateState state = new EndRoundExtendedDelegateState();
    state.superState = base;
    state.gameOver = true;
    state.winners = java.util.List.of(gameData.getPlayerList().getPlayers().get(0));
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new EndRoundStateSaver(), sampleState(), gameData);
  }
}
