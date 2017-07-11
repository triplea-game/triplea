package games.strategy.persistence.serializable;

public final class DefaultProxyFactoryRegistryAsProxyFactoryRegistryTest extends AbstractProxyFactoryRegistryTestCase {
  @Override
  protected ProxyFactoryRegistry createProxyFactoryRegistry() {
    return new DefaultProxyFactoryRegistry();
  }
}
