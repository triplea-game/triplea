package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class DefaultProxyRegistry implements ProxyRegistry {
  private static final ProxyFactory IDENTITY_PROXY_FACTORY =
      ProxyFactory.newInstance(Object.class, Function.identity());

  private final Map<Class<?>, ProxyFactory> proxyFactoriesByPrincipalType;

  DefaultProxyRegistry(final Collection<ProxyFactory> proxyFactories) {
    proxyFactoriesByPrincipalType = proxyFactories.stream()
        .collect(Collectors.toMap(ProxyFactory::getPrincipalType, Function.identity()));
  }

  @Override
  public Object getProxyFor(final Object principal) {
    checkNotNull(principal);

    return getProxyFactory(principal.getClass()).newProxyFor(principal);
  }

  private ProxyFactory getProxyFactory(final Class<?> principalType) {
    return proxyFactoriesByPrincipalType.getOrDefault(principalType, IDENTITY_PROXY_FACTORY);
  }
}
