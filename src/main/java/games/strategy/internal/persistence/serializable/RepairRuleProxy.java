package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.IntegerMap;

/**
 * A serializable proxy for the {@link RepairRule} class.
 */
@Immutable
public final class RepairRuleProxy implements Proxy {
  private static final long serialVersionUID = 8614634243250168588L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(RepairRule.class, RepairRuleProxy::new);

  private final IntegerMap<Resource> costs;
  private final GameData gameData;
  private final String name;
  private final IntegerMap<NamedAttachable> results;

  public RepairRuleProxy(final RepairRule repairRule) {
    checkNotNull(repairRule);

    costs = repairRule.getCosts();
    gameData = repairRule.getData();
    name = repairRule.getName();
    results = repairRule.getResults();
  }

  @Override
  public Object readResolve() {
    return new RepairRule(name, gameData, results, costs);
  }
}
