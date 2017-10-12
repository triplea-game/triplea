package games.strategy.internal.persistence.serializable;

import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;

public final class DestroyerBombardTechAdvanceProxyAsProxyTest
    extends AbstractTechAdvanceProxyTestCase<DestroyerBombardTechAdvance> {
  public DestroyerBombardTechAdvanceProxyAsProxyTest() {
    super(DestroyerBombardTechAdvance.class, DestroyerBombardTechAdvance::new);
  }
}
