package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;

public final class ImprovedArtillerySupportAdvanceProxyAsProxyTest
    extends AbstractProxyTestCase<ImprovedArtillerySupportAdvance> {
  public ImprovedArtillerySupportAdvanceProxyAsProxyTest() {
    super(ImprovedArtillerySupportAdvance.class);
  }

  @Override
  protected Collection<ImprovedArtillerySupportAdvance> createPrincipals() {
    return Arrays.asList(newImprovedArtillerySupportAdvance());
  }

  private static ImprovedArtillerySupportAdvance newImprovedArtillerySupportAdvance() {
    final ImprovedArtillerySupportAdvance improvedArtillerySupportAdvance =
        new ImprovedArtillerySupportAdvance(TestGameDataFactory.newValidGameData());
    TestGameDataComponentFactory.initializeAttachable(improvedArtillerySupportAdvance);
    return improvedArtillerySupportAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.IMPROVED_ARTILLERY_SUPPORT_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(ImprovedArtillerySupportAdvanceProxy.FACTORY)
        .build();
  }
}
