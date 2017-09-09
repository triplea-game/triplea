package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.triplea.delegate.JetPowerAdvance;

public final class JetPowerAdvanceProxyAsProxyTest extends AbstractTechAdvanceProxyTestCase<JetPowerAdvance> {
  public JetPowerAdvanceProxyAsProxyTest() {
    super(
        JetPowerAdvance.class,
        JetPowerAdvance::new,
        EngineDataEqualityComparators.JET_POWER_ADVANCE,
        JetPowerAdvanceProxy.FACTORY);
  }
}
