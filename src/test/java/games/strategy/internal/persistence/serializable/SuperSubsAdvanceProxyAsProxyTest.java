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
import games.strategy.triplea.delegate.SuperSubsAdvance;

public final class SuperSubsAdvanceProxyAsProxyTest extends AbstractProxyTestCase<SuperSubsAdvance> {
  public SuperSubsAdvanceProxyAsProxyTest() {
    super(SuperSubsAdvance.class);
  }

  @Override
  protected Collection<SuperSubsAdvance> createPrincipals() {
    return Arrays.asList(newSuperSubsAdvance());
  }

  private static SuperSubsAdvance newSuperSubsAdvance() {
    final SuperSubsAdvance superSubsAdvance = new SuperSubsAdvance(TestGameDataFactory.newValidGameData());
    TestGameDataComponentFactory.initializeAttachable(superSubsAdvance);
    return superSubsAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.SUPER_SUBS_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(SuperSubsAdvanceProxy.FACTORY)
        .build();
  }
}
