package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.IntegerMap;
import net.jcip.annotations.Immutable;

/**
 * A serializable proxy for the {@link ResourceCollection} class.
 */
@Immutable
public final class ResourceCollectionProxy implements Proxy {
  private static final long serialVersionUID = -513052202453337139L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(ResourceCollection.class, ResourceCollectionProxy::new);

  private final GameData gameData;
  private final IntegerMap<Resource> resources;

  public ResourceCollectionProxy(final ResourceCollection resourceCollection) {
    checkNotNull(resourceCollection);

    gameData = resourceCollection.getData();
    resources = resourceCollection.getResourcesCopy();
  }

  @Override
  public Object readResolve() {
    return new ResourceCollection(gameData, resources);
  }
}
