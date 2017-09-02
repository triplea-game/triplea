package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;

public final class DestroyerBombardTechAdvanceProxyAsProxyTest
    extends AbstractTechAdvanceProxyTestCase<DestroyerBombardTechAdvance> {
  public DestroyerBombardTechAdvanceProxyAsProxyTest() {
    super(
        DestroyerBombardTechAdvance.class,
        DestroyerBombardTechAdvance::new,
        EngineDataEqualityComparators.DESTROYER_BOMBARD_TECH_ADVANCE,
        DestroyerBombardTechAdvanceProxy.FACTORY);
  }
}
