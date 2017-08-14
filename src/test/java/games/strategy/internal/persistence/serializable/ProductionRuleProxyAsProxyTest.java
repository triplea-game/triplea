package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.data.Matchers.equalToProductionRule;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.IntegerMap;

public final class ProductionRuleProxyAsProxyTest extends AbstractProxyTestCase<ProductionRule> {
  public ProductionRuleProxyAsProxyTest() {
    super(ProductionRule.class);
  }

  @Override
  protected void assertPrincipalEquals(final ProductionRule expected, final ProductionRule actual) {
    checkNotNull(expected);
    checkNotNull(actual);

    assertThat(actual, is(equalToProductionRule(expected)));
  }

  @Override
  protected Collection<ProductionRule> createPrincipals() {
    return Arrays.asList(newProductionRule());
  }

  private static ProductionRule newProductionRule() {
    final GameData gameData = TestGameDataFactory.newValidGameData();
    final IntegerMap<NamedAttachable> resources = new IntegerMap<>();
    resources.add(TestGameDataComponentFactory.newResource(gameData, "resource1"), 11);
    resources.add(TestGameDataComponentFactory.newResource(gameData, "resource2"), 22);
    final IntegerMap<Resource> costs = new IntegerMap<>();
    costs.add(TestGameDataComponentFactory.newResource(gameData, "resource3"), 33);
    costs.add(TestGameDataComponentFactory.newResource(gameData, "resource4"), 44);
    return new ProductionRule("productionRule", gameData, resources, costs);
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
