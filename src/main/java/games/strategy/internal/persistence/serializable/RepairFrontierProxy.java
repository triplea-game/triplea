package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.RepairFrontier;
import games.strategy.engine.data.RepairRule;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link RepairFrontier} class.
 */
@Immutable
public final class RepairFrontierProxy implements Proxy {
  private static final long serialVersionUID = -2983755741123633255L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(RepairFrontier.class, RepairFrontierProxy::new);

  private final GameData gameData;
  private final String name;
  private final List<RepairRule> rules;

  public RepairFrontierProxy(final RepairFrontier repairFrontier) {
    checkNotNull(repairFrontier);

    gameData = repairFrontier.getData();
    name = repairFrontier.getName();
    rules = repairFrontier.getRules();
  }

  @Override
  public Object readResolve() {
    return new RepairFrontier(name, gameData, rules);
  }
}
