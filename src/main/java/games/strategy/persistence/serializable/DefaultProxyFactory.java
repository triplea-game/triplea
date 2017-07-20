package games.strategy.persistence.serializable;

import java.util.function.Function;

import net.jcip.annotations.Immutable;

@Immutable
final class DefaultProxyFactory<T> implements ProxyFactory {
  private final Function<T, ?> newProxyForPrincipal;
  private final Class<T> principalType;

  DefaultProxyFactory(final Class<T> principalType, final Function<T, ?> newProxyFor) {
    this.newProxyForPrincipal = newProxyFor;
    this.principalType = principalType;
  }

  @Override
  public Class<?> getPrincipalType() {
    return principalType;
  }

  @Override
  public Object newProxyFor(final Object principal) {
    return newProxyForPrincipal.apply(principalType.cast(principal));
  }
}
