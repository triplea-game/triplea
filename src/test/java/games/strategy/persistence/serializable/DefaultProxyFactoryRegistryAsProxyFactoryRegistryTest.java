package games.strategy.persistence.serializable;

import java.util.Collection;

public final class DefaultProxyFactoryRegistryAsProxyFactoryRegistryTest extends AbstractProxyFactoryRegistryTestCase {
  @Override
  protected ProxyFactoryRegistry createProxyFactoryRegistry(final Collection<ProxyFactory> proxyFactories) {
    return new DefaultProxyFactoryRegistry(proxyFactories);
  }
}
