package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newProductionFrontier;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.ProductionFrontier;

public final class ProductionFrontierProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<ProductionFrontier> {
  public ProductionFrontierProxyAsProxyTest() {
    super(ProductionFrontier.class);
  }

  @Override
  protected Collection<ProductionFrontier> createPrincipals() {
    return Arrays.asList(newProductionFrontier(getGameData(), "productionFrontier"));
  }
}
