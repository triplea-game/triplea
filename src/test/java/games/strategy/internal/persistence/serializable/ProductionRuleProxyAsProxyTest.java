package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

public final class ProductionRuleProxyAsProxyTest extends AbstractProxyTestCase<ProductionRule> {
  public ProductionRuleProxyAsProxyTest() {
    super(ProductionRule.class);
  }

  @Override
  protected Collection<ProductionRule> createPrincipals() {
    return Arrays.asList(TestGameDataComponentFactory.newProductionRule(
        TestGameDataFactory.newValidGameData(),
        "productionRule"));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(CoreEqualityComparators.INTEGER_MAP)
        .add(EngineDataEqualityComparators.PRODUCTION_RULE)
        .add(EngineDataEqualityComparators.RESOURCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(IntegerMapProxy.FACTORY)
        .add(ProductionRuleProxy.FACTORY)
        .add(ResourceProxy.FACTORY)
        .build();
  }
}
