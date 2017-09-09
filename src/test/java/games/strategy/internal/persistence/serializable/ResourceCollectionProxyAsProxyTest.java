package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newResource;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

public final class ResourceCollectionProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<ResourceCollection> {
  public ResourceCollectionProxyAsProxyTest() {
    super(ResourceCollection.class);
  }

  @Override
  protected ResourceCollection newGameDataComponent(final GameData gameData) {
    final ResourceCollection resourceCollection = new ResourceCollection(gameData);
    resourceCollection.addResource(newResource(gameData, "resource1"), 11);
    resourceCollection.addResource(newResource(gameData, "resource2"), 22);
    return resourceCollection;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        CoreEqualityComparators.INTEGER_MAP,
        EngineDataEqualityComparators.RESOURCE,
        EngineDataEqualityComparators.RESOURCE_COLLECTION);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        IntegerMapProxy.FACTORY,
        ResourceProxy.FACTORY,
        ResourceCollectionProxy.FACTORY);
  }
}
