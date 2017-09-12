package games.strategy.internal.persistence.serializable;

import games.strategy.triplea.delegate.IncreasedFactoryProductionAdvance;

public final class IncreasedFactoryProductionAdvanceProxyAsProxyTest
    extends AbstractTechAdvanceProxyTestCase<IncreasedFactoryProductionAdvance> {
  public IncreasedFactoryProductionAdvanceProxyAsProxyTest() {
    super(IncreasedFactoryProductionAdvance.class, IncreasedFactoryProductionAdvance::new);
  }
}
