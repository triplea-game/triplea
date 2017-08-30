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
import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;

public final class DestroyerBombardTechAdvanceProxyAsProxyTest
    extends AbstractProxyTestCase<DestroyerBombardTechAdvance> {
  public DestroyerBombardTechAdvanceProxyAsProxyTest() {
    super(DestroyerBombardTechAdvance.class);
  }

  @Override
  protected Collection<DestroyerBombardTechAdvance> createPrincipals() {
    return Arrays.asList(newDestroyerBombardTechAdvance());
  }

  private static DestroyerBombardTechAdvance newDestroyerBombardTechAdvance() {
    final DestroyerBombardTechAdvance destroyerBombardTechAdvance =
        new DestroyerBombardTechAdvance(TestGameDataFactory.newValidGameData());
    TestGameDataComponentFactory.initializeAttachable(destroyerBombardTechAdvance);
    return destroyerBombardTechAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.DESTROYER_BOMBARD_TECH_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(DestroyerBombardTechAdvanceProxy.FACTORY)
        .build();
  }
}
