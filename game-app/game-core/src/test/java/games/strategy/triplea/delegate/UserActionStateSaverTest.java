package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/** Oracle-gated round-trip test for {@link UserActionStateSaver}. */
class UserActionStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private UserActionExtendedDelegateState sampleState() {
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    base.endBaseStepsFinished = true;

    final UserActionExtendedDelegateState state = new UserActionExtendedDelegateState();
    state.superState = base;
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new UserActionStateSaver(), sampleState(), gameData);
  }
}
