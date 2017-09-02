package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.triplea.delegate.FakeTechAdvance;

public final class FakeTechAdvanceProxyAsProxyTest extends AbstractTechAdvanceProxyTestCase<FakeTechAdvance> {
  public FakeTechAdvanceProxyAsProxyTest() {
    super(
        FakeTechAdvance.class,
        FakeTechAdvanceProxyAsProxyTest::newFakeTechAdvance,
        EngineDataEqualityComparators.FAKE_TECH_ADVANCE,
        FakeTechAdvanceProxy.FACTORY);
  }

  private static FakeTechAdvance newFakeTechAdvance(final GameData gameData) {
    return TestGameDataComponentFactory.newFakeTechAdvance(gameData, "Tech Advance");
  }
}
