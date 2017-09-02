package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;

public final class ImprovedArtillerySupportAdvanceProxyAsProxyTest
    extends AbstractTechAdvanceProxyTestCase<ImprovedArtillerySupportAdvance> {
  public ImprovedArtillerySupportAdvanceProxyAsProxyTest() {
    super(
        ImprovedArtillerySupportAdvance.class,
        ImprovedArtillerySupportAdvance::new,
        EngineDataEqualityComparators.IMPROVED_ARTILLERY_SUPPORT_ADVANCE,
        ImprovedArtillerySupportAdvanceProxy.FACTORY);
  }
}
