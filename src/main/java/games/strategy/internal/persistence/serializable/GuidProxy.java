package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.rmi.dgc.VMID;

import javax.annotation.concurrent.Immutable;

import games.strategy.net.GUID;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link GUID} class.
 */
@Immutable
public final class GuidProxy implements Proxy {
  private static final long serialVersionUID = -9043396346551185991L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(GUID.class, GuidProxy::new);

  private final int id;
  private final VMID prefix;

  public GuidProxy(final GUID guid) {
    checkNotNull(guid);

    id = guid.getId();
    prefix = guid.getPrefix();
  }

  @Override
  public Object readResolve() {
    return new GUID(prefix, id);
  }
}
