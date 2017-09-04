package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.triplea.delegate.WarBondsAdvance;

public final class WarBondsAdvanceProxyAsProxyTest extends AbstractTechAdvanceProxyTestCase<WarBondsAdvance> {
  public WarBondsAdvanceProxyAsProxyTest() {
    super(
        WarBondsAdvance.class,
        WarBondsAdvance::new,
        EngineDataEqualityComparators.WAR_BONDS_ADVANCE,
        WarBondsAdvanceProxy.FACTORY);
  }
}
