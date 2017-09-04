package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newProductionRule;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionRule;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

public final class ProductionRuleProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<ProductionRule> {
  public ProductionRuleProxyAsProxyTest() {
    super(ProductionRule.class);
  }

  @Override
  protected ProductionRule newGameDataComponent(final GameData gameData) {
    return newProductionRule(gameData, "productionRule");
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        CoreEqualityComparators.INTEGER_MAP,
        EngineDataEqualityComparators.PRODUCTION_RULE,
        EngineDataEqualityComparators.RESOURCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        IntegerMapProxy.FACTORY,
        ProductionRuleProxy.FACTORY,
        ResourceProxy.FACTORY);
  }
}
