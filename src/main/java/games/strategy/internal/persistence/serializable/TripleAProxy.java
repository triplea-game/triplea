package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.TripleA;
import net.jcip.annotations.Immutable;

/**
 * A serializable proxy for the {@link TripleA} class.
 */
@Immutable
public final class TripleAProxy implements Proxy {
  private static final long serialVersionUID = 263867233328100619L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(TripleA.class, TripleAProxy::new);

  public TripleAProxy(final TripleA triplea) {
    checkNotNull(triplea);

    // do nothing; no persistent state
  }

  @Override
  public Object readResolve() {
    return new TripleA();
  }
}
