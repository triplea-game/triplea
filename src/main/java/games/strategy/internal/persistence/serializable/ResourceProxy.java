package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.Resource;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link Resource} class.
 */
@Immutable
public final class ResourceProxy implements Proxy {
  private static final long serialVersionUID = -314508780057279336L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(Resource.class, ResourceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;
  private final String name;

  public ResourceProxy(final Resource resource) {
    checkNotNull(resource);

    attachments = resource.getAttachments();
    gameData = resource.getData();
    name = resource.getName();
  }

  @Override
  public Object readResolve() {
    final Resource resource = new Resource(name, gameData);
    attachments.forEach(resource::addAttachment);
    return resource;
  }
}
