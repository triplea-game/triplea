package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class ResourceProxyAsProxyTest extends AbstractProxyTestCase<Resource> {
  public ResourceProxyAsProxyTest() {
    super(Resource.class);
  }

  @Override
  protected Collection<Resource> createPrincipals() {
    return Arrays.asList(TestGameDataComponentFactory.newResource(TestGameDataFactory.newValidGameData(), "resource"));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.RESOURCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(ResourceProxy.FACTORY)
        .build();
  }
}
