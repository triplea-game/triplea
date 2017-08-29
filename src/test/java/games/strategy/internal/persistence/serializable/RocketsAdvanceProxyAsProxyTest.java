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
import games.strategy.triplea.delegate.RocketsAdvance;

public final class RocketsAdvanceProxyAsProxyTest extends AbstractProxyTestCase<RocketsAdvance> {
  public RocketsAdvanceProxyAsProxyTest() {
    super(RocketsAdvance.class);
  }

  @Override
  protected Collection<RocketsAdvance> createPrincipals() {
    return Arrays.asList(TestGameDataComponentFactory.newRocketsAdvance(TestGameDataFactory.newValidGameData()));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.ROCKETS_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(RocketsAdvanceProxy.FACTORY)
        .build();
  }
}
