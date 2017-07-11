package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;

/**
 * A factory for creating a proxy that will be persisted in place of another object (the principal) within the Java
 * object serialization framework.
 *
 * <p>
 * The TripleA serialization streams use proxy factories to assist with serializing and deserializing objects,
 * especially those that are not naturally serializable (i.e. they do not implement {@code Serializable}). A proxy
 * factory registered for a specific type of principal will cause the output stream to substitute a new serializable
 * proxy whenever an instance of the principal type is encountered. Upon deserialization, the input stream will replace
 * the proxy with an instance of the principal type.
 * </p>
 *
 * <p>
 * To contribute a proxy factory for a specific class, register it with the platform's {@link ProxyFactoryRegistry}.
 * </p>
 */
public interface ProxyFactory {
  /**
   * Gets the type of the principal to be proxied.
   *
   * @return The type of the principal to be proxied; never {@code null}.
   */
  Class<?> getPrincipalType();

  /**
   * Creates a new serializable proxy for the specified principal.
   *
   * @param principal The principal to be proxied; must not be {@code null}.
   *
   * @return A new serializable proxy for the specified principal; never {@code null}.
   */
  Object newProxyFor(Object principal);

  /**
   * Creates a new proxy factory using the default implementation.
   *
   * @param principalType The type of the principal to be proxied; must not be {@code null}.
   * @param newProxyForPrincipal The function used to create a new proxy from a principal; must not be {@code null}.
   *
   * @return A new proxy factory; never {@code null}. The returned factory is immutable.
   */
  static <T> ProxyFactory newInstance(
      final Class<T> principalType,
      final Function<T, ?> newProxyForPrincipal) {
    checkNotNull(principalType);
    checkNotNull(newProxyForPrincipal);

    return new DefaultProxyFactory<>(principalType, newProxyForPrincipal);
  }
}
