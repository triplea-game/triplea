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
import games.strategy.triplea.delegate.IncreasedFactoryProductionAdvance;

public final class IncreasedFactoryProductionAdvanceProxyAsProxyTest
    extends AbstractProxyTestCase<IncreasedFactoryProductionAdvance> {
  public IncreasedFactoryProductionAdvanceProxyAsProxyTest() {
    super(IncreasedFactoryProductionAdvance.class);
  }

  @Override
  protected Collection<IncreasedFactoryProductionAdvance> createPrincipals() {
    return Arrays.asList(
        TestGameDataComponentFactory.newIncreasedFactoryProductionAdvance(TestGameDataFactory.newValidGameData()));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.INCREASED_FACTORY_PRODUCTION_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(IncreasedFactoryProductionAdvanceProxy.FACTORY)
        .build();
  }
}
