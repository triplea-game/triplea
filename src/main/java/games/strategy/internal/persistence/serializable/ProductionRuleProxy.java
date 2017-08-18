package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.IntegerMap;
import net.jcip.annotations.Immutable;

/**
 * A serializable proxy for the {@link ProductionRule} class.
 */
@Immutable
public final class ProductionRuleProxy implements Proxy {
  private static final long serialVersionUID = 3541658838672456487L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(ProductionRule.class, ProductionRuleProxy::new);

  private final IntegerMap<Resource> costs;
  private final GameData gameData;
  private final String name;
  private final IntegerMap<NamedAttachable> results;

  public ProductionRuleProxy(final ProductionRule productionRule) {
    checkNotNull(productionRule);

    costs = productionRule.getCosts();
    gameData = productionRule.getData();
    name = productionRule.getName();
    results = productionRule.getResults();
  }

  @Override
  public Object readResolve() {
    return new ProductionRule(name, gameData, results, costs);
  }
}
