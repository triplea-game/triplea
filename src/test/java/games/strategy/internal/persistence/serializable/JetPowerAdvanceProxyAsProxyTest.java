package games.strategy.internal.persistence.serializable;

import games.strategy.triplea.delegate.JetPowerAdvance;

public final class JetPowerAdvanceProxyAsProxyTest extends AbstractTechAdvanceProxyTestCase<JetPowerAdvance> {
  public JetPowerAdvanceProxyAsProxyTest() {
    super(JetPowerAdvance.class, JetPowerAdvance::new);
  }
}
