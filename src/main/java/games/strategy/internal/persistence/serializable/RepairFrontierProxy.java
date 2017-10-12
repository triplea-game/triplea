package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.RepairFrontier;
import games.strategy.engine.data.RepairRule;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link RepairFrontier} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of {@link RepairFrontier}
 * created from this proxy will always have their game data set to {@code null}. Proxies that may compose instances of
 * this proxy are required to manually restore the game data in their {@code readResolve()} method via a
 * context-dependent mechanism.
 * </p>
 */
@Immutable
public final class RepairFrontierProxy implements Proxy {
  private static final long serialVersionUID = -2983755741123633255L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(RepairFrontier.class, RepairFrontierProxy::new);

  private final String name;
  private final List<RepairRule> rules;

  public RepairFrontierProxy(final RepairFrontier repairFrontier) {
    checkNotNull(repairFrontier);

    name = repairFrontier.getName();
    rules = repairFrontier.getRules();
  }

  @Override
  public Object readResolve() {
    return new RepairFrontier(name, null, rules);
  }
}
