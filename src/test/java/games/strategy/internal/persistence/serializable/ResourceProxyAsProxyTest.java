package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newResource;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.Resource;

public final class ResourceProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<Resource> {
  public ResourceProxyAsProxyTest() {
    super(Resource.class);
  }

  @Override
  protected Collection<Resource> createPrincipals() {
    return Arrays.asList(newResource(getGameData(), "resource"));
  }
}
