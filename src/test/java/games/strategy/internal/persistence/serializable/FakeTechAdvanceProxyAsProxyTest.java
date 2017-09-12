package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.triplea.delegate.FakeTechAdvance;

public final class FakeTechAdvanceProxyAsProxyTest extends AbstractTechAdvanceProxyTestCase<FakeTechAdvance> {
  public FakeTechAdvanceProxyAsProxyTest() {
    super(FakeTechAdvance.class, FakeTechAdvanceProxyAsProxyTest::newFakeTechAdvance);
  }

  private static FakeTechAdvance newFakeTechAdvance(final GameData gameData) {
    return TestGameDataComponentFactory.newFakeTechAdvance(gameData, "Tech Advance");
  }
}
