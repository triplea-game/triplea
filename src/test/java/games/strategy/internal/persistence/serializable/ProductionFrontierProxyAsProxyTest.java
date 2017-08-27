package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

public final class ProductionFrontierProxyAsProxyTest extends AbstractProxyTestCase<ProductionFrontier> {
  public ProductionFrontierProxyAsProxyTest() {
    super(ProductionFrontier.class);
  }

  @Override
  protected Collection<ProductionFrontier> createPrincipals() {
    return Arrays.asList(newProductionFrontier());
  }

  private static ProductionFrontier newProductionFrontier() {
    final GameData gameData = TestGameDataFactory.newValidGameData();
    return new ProductionFrontier("productionFrontier", gameData, Arrays.asList(
        TestGameDataComponentFactory.newProductionRule(gameData, "productionRule1"),
        TestGameDataComponentFactory.newProductionRule(gameData, "productionRule2")));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(CoreEqualityComparators.INTEGER_MAP)
        .add(EngineDataEqualityComparators.PRODUCTION_FRONTIER)
        .add(EngineDataEqualityComparators.PRODUCTION_RULE)
        .add(EngineDataEqualityComparators.RESOURCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(IntegerMapProxy.FACTORY)
        .add(ProductionFrontierProxy.FACTORY)
        .add(ProductionRuleProxy.FACTORY)
        .add(ResourceProxy.FACTORY)
        .build();
  }
}
