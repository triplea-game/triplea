package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class DefaultProxyFactoryRegistry implements ProxyFactoryRegistry {
  private final Map<Class<?>, ProxyFactory> proxyFactoriesByPrincipalType = new HashMap<>();

  @Override
  public Optional<ProxyFactory> getProxyFactory(final Class<?> principalType) {
    checkNotNull(principalType);

    return Optional.ofNullable(proxyFactoriesByPrincipalType.get(principalType));
  }

  @Override
  public Collection<ProxyFactory> getProxyFactories() {
    return new ArrayList<>(proxyFactoriesByPrincipalType.values());
  }

  @Override
  public void registerProxyFactory(final ProxyFactory proxyFactory) {
    checkNotNull(proxyFactory);
    final Class<?> principalType = proxyFactory.getPrincipalType();
    checkArgument(
        proxyFactoriesByPrincipalType.putIfAbsent(principalType, proxyFactory) == null,
        String.format("a proxy factory for principal type '%s' is already registered", principalType));
  }

  @Override
  public void unregisterProxyFactory(final ProxyFactory proxyFactory) {
    checkNotNull(proxyFactory);
    final Class<?> principalType = proxyFactory.getPrincipalType();
    checkArgument(
        proxyFactoriesByPrincipalType.remove(principalType, proxyFactory),
        String.format("the proxy factory was not previously registered for principal type '%s'", principalType));
  }
}
