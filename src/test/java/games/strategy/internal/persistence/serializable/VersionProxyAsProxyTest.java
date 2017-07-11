package games.strategy.internal.persistence.serializable;

import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactoryRegistry;
import games.strategy.util.Version;

public final class VersionProxyAsProxyTest extends AbstractProxyTestCase<Version> {
  public VersionProxyAsProxyTest() {
    super(Version.class);
  }

  @Override
  protected Version createPrincipal() {
    return new Version(1, 2, 3, 4);
  }

  @Override
  protected void registerProxyFactories(final ProxyFactoryRegistry proxyFactoryRegistry) {
    proxyFactoryRegistry.registerProxyFactory(VersionProxy.FACTORY);
  }
}
