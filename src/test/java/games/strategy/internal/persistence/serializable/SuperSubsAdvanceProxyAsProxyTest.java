package games.strategy.internal.persistence.serializable;

import games.strategy.triplea.delegate.SuperSubsAdvance;

public final class SuperSubsAdvanceProxyAsProxyTest extends AbstractTechAdvanceProxyTestCase<SuperSubsAdvance> {
  public SuperSubsAdvanceProxyAsProxyTest() {
    super(SuperSubsAdvance.class, SuperSubsAdvance::new);
  }
}
