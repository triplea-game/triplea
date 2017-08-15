package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;

public final class ResourceProxyAsProxyTest extends AbstractProxyTestCase<Resource> {
  public ResourceProxyAsProxyTest() {
    super(Resource.class);
  }

  @Override
  protected void assertPrincipalEquals(final Resource expected, final Resource actual) {
    assertThat(actual, is(equalTo(expected)));
  }

  @Override
  protected Collection<Resource> createPrincipals() {
    return Arrays.asList(TestGameDataComponentFactory.newResource(TestGameDataFactory.newValidGameData(), "resource"));
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(ResourceProxy.FACTORY)
        .build();
  }
}
