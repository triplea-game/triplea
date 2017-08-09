package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.IntegerMap;

public final class IntegerMapProxyAsProxyTest extends AbstractProxyTestCase<IntegerMap<?>> {
  @SuppressWarnings("unchecked")
  public IntegerMapProxyAsProxyTest() {
    super((Class<IntegerMap<?>>) (Class<?>) IntegerMap.class);
  }

  @Override
  protected Collection<IntegerMap<?>> createPrincipals() {
    final IntegerMap<String> integerMap = new IntegerMap<>();
    integerMap.add("a", 1);
    integerMap.add("b", 2);
    integerMap.add("c", 3);
    return Arrays.asList(integerMap);
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return Arrays.asList(IntegerMapProxy.FACTORY);
  }
}
