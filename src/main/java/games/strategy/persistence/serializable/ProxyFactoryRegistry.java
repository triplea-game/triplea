package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
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
   * Creates a new proxy factory registry for the specified array of proxy factories using the default implementation.
   *
   * @param proxyFactories The array of proxy factories to associate with the registry; must not be {@code null}.
   *
   * @return A new proxy factory registry; never {@code null}. The returned registry is immutable.
   */
  static ProxyFactoryRegistry newInstance(final ProxyFactory... proxyFactories) {
    checkNotNull(proxyFactories);

    return newInstance(Arrays.asList(proxyFactories));
  }

  /**
   * Creates a new proxy factory registry for the specified collection of proxy factories using the default
   * implementation.
   *
   * @param proxyFactories The collection of proxy factories to associate with the registry; must not be {@code null}.
   *
   * @return A new proxy factory registry; never {@code null}. The returned registry is immutable.
   */
  static ProxyFactoryRegistry newInstance(final Collection<ProxyFactory> proxyFactories) {
    checkNotNull(proxyFactories);

    return new DefaultProxyFactoryRegistry(proxyFactories);
  }
}
