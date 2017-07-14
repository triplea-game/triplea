package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;

/**
 * A factory for creating a serializable proxy from a (possibly) non-serializable principal.
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
  static <T> ProxyFactory newInstance(final Class<T> principalType, final Function<T, ?> newProxyForPrincipal) {
    checkNotNull(principalType);
    checkNotNull(newProxyForPrincipal);

    return new DefaultProxyFactory<>(principalType, newProxyForPrincipal);
  }
}
