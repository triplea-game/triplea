package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.IntegerMap;

/**
 * A serializable proxy for the {@link IntegerMap} class.
 */
@Immutable
public final class IntegerMapProxy implements Proxy {
  private static final long serialVersionUID = 1277651902932560180L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(IntegerMap.class, IntegerMapProxy::new);

  private final Map<?, Integer> map;

  public IntegerMapProxy(final IntegerMap<?> integerMap) {
    checkNotNull(integerMap);

    map = integerMap.toMap();
  }

  @Override
  public Object readResolve() {
    return new IntegerMap<>(map);
  }
}
