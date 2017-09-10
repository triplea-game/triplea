package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.TechAdvance;

/**
 * A serializable proxy for the {@link TechnologyFrontier} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link TechnologyFrontier} created from this proxy will always have their game data set to {@code null}. Proxies that
 * may compose instances of this proxy are required to manually restore the game data in their {@code readResolve()}
 * method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class TechnologyFrontierProxy implements Proxy {
  private static final long serialVersionUID = 5288910939762147346L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(TechnologyFrontier.class, TechnologyFrontierProxy::new);

  private final String name;
  private final List<TechAdvance> techAdvances;

  public TechnologyFrontierProxy(final TechnologyFrontier technologyFrontier) {
    checkNotNull(technologyFrontier);

    name = technologyFrontier.getName();
    techAdvances = technologyFrontier.getTechs();
  }

  @Override
  public Object readResolve() {
    final TechnologyFrontier technologyFrontier = new TechnologyFrontier(name, null);
    technologyFrontier.addAdvance(techAdvances);
    return technologyFrontier;
  }
}
