package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * A service for obtaining a proxy that can be persisted in place of another object (the principal) within the Java
 * object serialization framework.
 *
 * <p>
 * The TripleA serialization streams use a proxy registry to assist with serializing and deserializing objects,
 * especially those that are not naturally serializable (i.e. they do not implement {@code Serializable}). A proxy
 * factory registered for a specific type of principal will cause the output stream to substitute a new serializable
 * proxy whenever an instance of the principal type is encountered. Upon deserialization, the input stream will replace
 * the proxy with an instance of the principal type.
 * </p>
 */
public interface ProxyRegistry {
  /**
   * Gets a serializable proxy for the specified principal.
   *
   * @param principal The principal to be proxied; must not be {@code null}.
   *
   * @return A serializable proxy for the specified principal; never {@code null}. If a proxy factory for the
   *         principal's type has not been registered, the principal itself will be returned.
   */
  Object getProxyFor(Object principal);

  /**
   * Creates a new proxy registry for the specified array of proxy factories using the default implementation.
   *
   * @param proxyFactories The array of proxy factories to associate with the registry; must not be {@code null}.
   *
   * @return A new proxy registry; never {@code null}. The returned registry is immutable.
   */
  static ProxyRegistry newInstance(final ProxyFactory... proxyFactories) {
    checkNotNull(proxyFactories);

    return newInstance(Arrays.asList(proxyFactories));
  }

  /**
   * Creates a new proxy registry for the specified collection of proxy factories using the default implementation.
   *
   * @param proxyFactories The collection of proxy factories to associate with the registry; must not be {@code null}.
   *
   * @return A new proxy registry; never {@code null}. The returned registry is immutable.
   */
  static ProxyRegistry newInstance(final Collection<ProxyFactory> proxyFactories) {
    checkNotNull(proxyFactories);

    return new DefaultProxyRegistry(proxyFactories);
  }
}
