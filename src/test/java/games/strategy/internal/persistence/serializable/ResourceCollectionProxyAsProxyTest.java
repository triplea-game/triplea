package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;

public final class ResourceCollectionProxyAsProxyTest extends AbstractProxyTestCase<ResourceCollection> {
  public ResourceCollectionProxyAsProxyTest() {
    super(ResourceCollection.class);
  }

  @Override
  protected void assertPrincipalEquals(final ResourceCollection expected, final ResourceCollection actual) {
    assertThat(actual, is(equalTo(expected)));
  }

  @Override
  protected Collection<ResourceCollection> createPrincipals() {
    return Arrays.asList(newResourceCollection());
  }

  private static ResourceCollection newResourceCollection() {
    final GameData gameData = TestGameDataFactory.newValidGameData();
    final ResourceCollection resourceCollection = new ResourceCollection(gameData);
    resourceCollection.addResource(TestGameDataComponentFactory.newResource(gameData, "resource1"), 11);
    resourceCollection.addResource(TestGameDataComponentFactory.newResource(gameData, "resource2"), 22);
    return resourceCollection;
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(IntegerMapProxy.FACTORY)
        .add(ResourceProxy.FACTORY)
        .add(ResourceCollectionProxy.FACTORY)
        .build();
  }
}
