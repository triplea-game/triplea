package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.IntegerMap;

/**
 * A serializable proxy for the {@link ProductionRule} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of {@link ProductionRule}
 * created from this proxy will always have their game data set to {@code null}. Proxies that may compose instances of
 * this proxy are required to manually restore the game data in their {@code readResolve()} method via a
 * context-dependent mechanism.
 * </p>
 */
@Immutable
public final class ProductionRuleProxy implements Proxy {
  private static final long serialVersionUID = 3541658838672456487L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(ProductionRule.class, ProductionRuleProxy::new);

  private final IntegerMap<Resource> costs;
  private final String name;
  private final IntegerMap<NamedAttachable> results;

  public ProductionRuleProxy(final ProductionRule productionRule) {
    checkNotNull(productionRule);

    costs = productionRule.getCosts();
    name = productionRule.getName();
    results = productionRule.getResults();
  }

  @Override
  public Object readResolve() {
    return new ProductionRule(name, null, results, costs);
  }
}
