package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;

public final class ImprovedArtillerySupportAdvanceProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<ImprovedArtillerySupportAdvance> {
  public ImprovedArtillerySupportAdvanceProxyAsProxyTest() {
    super(ImprovedArtillerySupportAdvance.class);
  }

  @Override
  protected ImprovedArtillerySupportAdvance newGameDataComponent(final GameData gameData) {
    final ImprovedArtillerySupportAdvance improvedArtillerySupportAdvance =
        new ImprovedArtillerySupportAdvance(gameData);
    initializeAttachable(improvedArtillerySupportAdvance);
    return improvedArtillerySupportAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.IMPROVED_ARTILLERY_SUPPORT_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(ImprovedArtillerySupportAdvanceProxy.FACTORY);
  }
}
