package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.triplea.delegate.ParatroopersAdvance;

public final class ParatroopersAdvanceProxyAsProxyTest extends AbstractTechAdvanceProxyTestCase<ParatroopersAdvance> {
  public ParatroopersAdvanceProxyAsProxyTest() {
    super(
        ParatroopersAdvance.class,
        ParatroopersAdvance::new,
        EngineDataEqualityComparators.PARATROOPERS_ADVANCE,
        ParatroopersAdvanceProxy.FACTORY);
  }
}
