package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.triplea.delegate.IndustrialTechnologyAdvance;

public final class IndustrialTechnologyAdvanceProxyAsProxyTest
    extends AbstractTechAdvanceProxyTestCase<IndustrialTechnologyAdvance> {
  public IndustrialTechnologyAdvanceProxyAsProxyTest() {
    super(
        IndustrialTechnologyAdvance.class,
        IndustrialTechnologyAdvance::new,
        EngineDataEqualityComparators.INDUSTRIAL_TECHNOLOGY_ADVANCE,
        IndustrialTechnologyAdvanceProxy.FACTORY);
  }
}
