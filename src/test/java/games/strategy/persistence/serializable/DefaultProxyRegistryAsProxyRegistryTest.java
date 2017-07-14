package games.strategy.persistence.serializable;

import java.util.Collection;

public final class DefaultProxyRegistryAsProxyRegistryTest extends AbstractProxyRegistryTestCase {
  @Override
  protected ProxyRegistry createProxyRegistry(final Collection<ProxyFactory> proxyFactories) {
    return new DefaultProxyRegistry(proxyFactories);
  }
}
