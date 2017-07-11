package games.strategy.persistence.serializable;

import java.util.Collection;
import java.util.Optional;

/**
 * A service for the management and discovery of Java object serialization framework proxy factories.
 */
public interface ProxyFactoryRegistry {
  /**
   * Gets the proxy factory associated with the specified principal type.
   *
   * @param principalType The type of principal to be proxied; must not be {@code null}.
   *
   * @return The proxy factory associated with the specified principal type or empty if no such principal type is
   *         registered.
   */
  Optional<ProxyFactory> getProxyFactory(Class<?> principalType);

  /**
   * Gets a collection of all proxy factories that have been registered with this service.
   *
   * @return A collection of all proxy factories that have been registered with this service; never {@code null}. This
   *         collection is a snapshot of the proxy factories registered at the time of the call.
   */
  Collection<ProxyFactory> getProxyFactories();

  /**
   * Registers the specified proxy factory.
   *
   * @param proxyFactory The proxy factory; must not be {@code null}.
   *
   * @throws IllegalArgumentException If a proxy factory is already registered with the same principal type.
   */
  void registerProxyFactory(ProxyFactory proxyFactory);

  /**
   * Unregisters the specified proxy factory.
   *
   * @param proxyFactory The proxy factory; must not be {@code null}.
   *
   * @throws IllegalArgumentException If the specified proxy factory was not previously registered.
   */
  void unregisterProxyFactory(ProxyFactory proxyFactory);

  /**
   * Creates a new proxy factory registry using the default implementation.
   *
   * @return A new proxy factory registry; never {@code null}. The returned registry is not thread safe.
   */
  static ProxyFactoryRegistry newInstance() {
    return new DefaultProxyFactoryRegistry();
  }
}
