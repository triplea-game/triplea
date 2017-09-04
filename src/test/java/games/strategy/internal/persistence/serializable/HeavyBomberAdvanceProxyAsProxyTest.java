package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.triplea.delegate.HeavyBomberAdvance;

public final class HeavyBomberAdvanceProxyAsProxyTest extends AbstractTechAdvanceProxyTestCase<HeavyBomberAdvance> {
  public HeavyBomberAdvanceProxyAsProxyTest() {
    super(
        HeavyBomberAdvance.class,
        HeavyBomberAdvance::new,
        EngineDataEqualityComparators.HEAVY_BOMBER_ADVANCE,
        HeavyBomberAdvanceProxy.FACTORY);
  }
}
