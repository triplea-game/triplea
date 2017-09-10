package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link ProductionFrontier} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link ProductionFrontier} created from this proxy will always have their game data set to {@code null}. Proxies that
 * may compose instances of this proxy are required to manually restore the game data in their {@code readResolve()}
 * method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class ProductionFrontierProxy implements Proxy {
  private static final long serialVersionUID = 4785506781105056191L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(ProductionFrontier.class, ProductionFrontierProxy::new);

  private final String name;
  private final List<ProductionRule> rules;

  public ProductionFrontierProxy(final ProductionFrontier productionFrontier) {
    checkNotNull(productionFrontier);

    name = productionFrontier.getName();
    rules = productionFrontier.getRules();
  }

  @Override
  public Object readResolve() {
    return new ProductionFrontier(name, null, rules);
  }
}
