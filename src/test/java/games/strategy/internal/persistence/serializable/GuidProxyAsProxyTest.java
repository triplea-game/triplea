package games.strategy.internal.persistence.serializable;

import java.rmi.dgc.VMID;
import java.util.Arrays;
import java.util.Collection;

import games.strategy.net.GUID;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;

public final class GuidProxyAsProxyTest extends AbstractProxyTestCase<GUID> {
  public GuidProxyAsProxyTest() {
    super(GUID.class);
  }

  @Override
  protected Collection<GUID> createPrincipals() {
    return Arrays.asList(new GUID(new VMID(), 42));
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return Arrays.asList(GuidProxy.FACTORY);
  }
}
