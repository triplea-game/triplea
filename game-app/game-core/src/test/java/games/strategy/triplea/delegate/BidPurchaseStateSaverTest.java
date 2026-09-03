package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/** Oracle-gated round-trip test for {@link BidPurchaseStateSaver}. */
class BidPurchaseStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private BidPurchaseExtendedDelegateState sampleState() {
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    base.endBaseStepsFinished = false;

    final BidPurchaseExtendedDelegateState state = new BidPurchaseExtendedDelegateState();
    state.superState = base;
    state.bid = 12;
    state.spent = 5;
    state.hasBid = true;
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new BidPurchaseStateSaver(), sampleState(), gameData);
  }
}
