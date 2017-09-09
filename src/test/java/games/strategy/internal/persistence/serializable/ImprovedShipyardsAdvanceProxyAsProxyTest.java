package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.triplea.delegate.ImprovedShipyardsAdvance;

public final class ImprovedShipyardsAdvanceProxyAsProxyTest
    extends AbstractTechAdvanceProxyTestCase<ImprovedShipyardsAdvance> {
  public ImprovedShipyardsAdvanceProxyAsProxyTest() {
    super(
        ImprovedShipyardsAdvance.class,
        ImprovedShipyardsAdvance::new,
        EngineDataEqualityComparators.IMPROVED_SHIPYARDS_ADVANCE,
        ImprovedShipyardsAdvanceProxy.FACTORY);
  }
}
