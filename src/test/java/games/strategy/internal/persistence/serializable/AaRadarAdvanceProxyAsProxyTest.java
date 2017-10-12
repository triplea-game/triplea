package games.strategy.internal.persistence.serializable;

import games.strategy.triplea.delegate.AARadarAdvance;

public final class AaRadarAdvanceProxyAsProxyTest extends AbstractTechAdvanceProxyTestCase<AARadarAdvance> {
  public AaRadarAdvanceProxyAsProxyTest() {
    super(AARadarAdvance.class, AARadarAdvance::new);
  }
}
