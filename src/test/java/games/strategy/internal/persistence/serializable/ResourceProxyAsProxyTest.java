package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newResource;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Resource;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class ResourceProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<Resource> {
  public ResourceProxyAsProxyTest() {
    super(Resource.class);
  }

  @Override
  protected Resource newGameDataComponent(final GameData gameData) {
    return newResource(gameData, "resource");
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.RESOURCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(ResourceProxy.FACTORY);
  }
}
