package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newProductionFrontier;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

public final class ProductionFrontierProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<ProductionFrontier> {
  public ProductionFrontierProxyAsProxyTest() {
    super(ProductionFrontier.class);
  }

  @Override
  protected Collection<ProductionFrontier> createPrincipals() {
    return Arrays.asList(newProductionFrontier(getGameData(), "productionFrontier"));
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        CoreEqualityComparators.INTEGER_MAP,
        EngineDataEqualityComparators.PRODUCTION_FRONTIER,
        EngineDataEqualityComparators.PRODUCTION_RULE,
        EngineDataEqualityComparators.RESOURCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        IntegerMapProxy.FACTORY,
        ProductionFrontierProxy.FACTORY,
        ProductionRuleProxy.FACTORY,
        ResourceProxy.FACTORY);
  }
}
