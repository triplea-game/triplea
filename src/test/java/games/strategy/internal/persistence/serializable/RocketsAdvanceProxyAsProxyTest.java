package games.strategy.internal.persistence.serializable;

import games.strategy.triplea.delegate.RocketsAdvance;

public final class RocketsAdvanceProxyAsProxyTest extends AbstractTechAdvanceProxyTestCase<RocketsAdvance> {
  public RocketsAdvanceProxyAsProxyTest() {
    super(RocketsAdvance.class, RocketsAdvance::new);
  }
}
