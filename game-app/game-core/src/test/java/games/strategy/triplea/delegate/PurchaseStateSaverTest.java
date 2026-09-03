package games.strategy.triplea.delegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

/**
 * Oracle-gated test for the {@link PurchaseStateSaver} reference implementation: it exercises the
 * whole foundation — the reference scheme (ProductionRule by name), the nested {@code superState}
 * blob fallback, and the full text save/load pipeline with a native delegate saver active.
 */
class PurchaseStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private PurchaseExtendedDelegateState sampleState() {
    final ProductionRule rule =
        gameData.getProductionRuleList().getProductionRules().iterator().next();
    final IntegerMap<ProductionRule> pending = new IntegerMap<>();
    pending.put(rule, 3);

    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    base.endBaseStepsFinished = false;

    final PurchaseExtendedDelegateState state = new PurchaseExtendedDelegateState();
    state.superState = base;
    state.needToInitialize = true;
    state.pendingProductionRules = pending;
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new PurchaseStateSaver(), sampleState(), gameData);
  }

  @Test
  void fullTextSaveRoundTripsPurchaseDelegateNatively() {
    final PurchaseDelegate purchase = findPurchaseDelegate(gameData);
    purchase.loadState(sampleState());

    final GameData reloaded = GameDataOracle.assertReconstructs(gameData);

    assertThat(findPurchaseDelegate(reloaded), is(notNullValue()));
  }

  private static PurchaseDelegate findPurchaseDelegate(final GameData data) {
    return data.getDelegates().stream()
        .filter(PurchaseDelegate.class::isInstance)
        .map(PurchaseDelegate.class::cast)
        .findFirst()
        .orElseThrow(() -> new AssertionError("no PurchaseDelegate in fixture"));
  }
}
