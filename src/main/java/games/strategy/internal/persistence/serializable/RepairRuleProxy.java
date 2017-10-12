package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.IntegerMap;

/**
 * A serializable proxy for the {@link RepairRule} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of {@link RepairRule}
 * created from this proxy will always have their game data set to {@code null}. Proxies that may compose instances of
 * this proxy are required to manually restore the game data in their {@code readResolve()} method via a
 * context-dependent mechanism.
 * </p>
 */
@Immutable
public final class RepairRuleProxy implements Proxy {
  private static final long serialVersionUID = 8614634243250168588L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(RepairRule.class, RepairRuleProxy::new);

  private final IntegerMap<Resource> costs;
  private final String name;
  private final IntegerMap<NamedAttachable> results;

  public RepairRuleProxy(final RepairRule repairRule) {
    checkNotNull(repairRule);

    costs = repairRule.getCosts();
    name = repairRule.getName();
    results = repairRule.getResults();
  }

  @Override
  public Object readResolve() {
    return new RepairRule(name, null, results, costs);
  }
}
