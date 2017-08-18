package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import net.jcip.annotations.Immutable;

/**
 * A serializable proxy for the {@link ProductionFrontier} class.
 */
@Immutable
public final class ProductionFrontierProxy implements Proxy {
  private static final long serialVersionUID = 4785506781105056191L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(ProductionFrontier.class, ProductionFrontierProxy::new);

  private final GameData gameData;
  private final String name;
  private final List<ProductionRule> rules;

  public ProductionFrontierProxy(final ProductionFrontier productionFrontier) {
    checkNotNull(productionFrontier);

    gameData = productionFrontier.getData();
    name = productionFrontier.getName();
    rules = productionFrontier.getRules();
  }

  @Override
  public Object readResolve() {
    final ProductionFrontier productionFrontier = new ProductionFrontier(name, gameData);
    rules.forEach(productionFrontier::addRule);
    return productionFrontier;
  }
}
