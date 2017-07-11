package games.strategy.engine.persistence.serializable;

import games.strategy.internal.persistence.serializable.PropertyBagMementoProxy;
import games.strategy.internal.persistence.serializable.VersionProxy;
import games.strategy.persistence.serializable.ProxyFactoryRegistry;

/**
 * Factory for creating pre-configured instances of {@link ProxyFactoryRegistry}.
 */
public final class ProxyFactoryRegistryFactory {
  private ProxyFactoryRegistryFactory() {}

  /**
   * Creates a new proxy factory registry that has been configured with all platform proxy factories.
   *
   * @return A new proxy factory registry; never {@code null}.
   */
  public static ProxyFactoryRegistry newPlatformProxyFactoryRegistry() {
    final ProxyFactoryRegistry proxyFactoryRegistry = ProxyFactoryRegistry.newInstance();
    proxyFactoryRegistry.registerProxyFactory(PropertyBagMementoProxy.FACTORY);
    proxyFactoryRegistry.registerProxyFactory(VersionProxy.FACTORY);
    return proxyFactoryRegistry;
  }
}
