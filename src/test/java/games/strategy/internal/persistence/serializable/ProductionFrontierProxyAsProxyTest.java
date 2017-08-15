package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;

public final class ProductionFrontierProxyAsProxyTest extends AbstractProxyTestCase<ProductionFrontier> {
  public ProductionFrontierProxyAsProxyTest() {
    super(ProductionFrontier.class);
  }

  @Override
  protected void assertPrincipalEquals(final ProductionFrontier expected, final ProductionFrontier actual) {
    assertThat(actual, is(equalTo(expected)));
  }

  @Override
  protected Collection<ProductionFrontier> createPrincipals() {
    return Arrays.asList(newProductionFrontier());
  }

  private static ProductionFrontier newProductionFrontier() {
    final GameData gameData = TestGameDataFactory.newValidGameData();
    final ProductionFrontier productionFrontier = new ProductionFrontier("productionFrontier", gameData);
    productionFrontier.addRule(TestGameDataComponentFactory.newProductionRule(gameData, "productionRule1"));
    productionFrontier.addRule(TestGameDataComponentFactory.newProductionRule(gameData, "productionRule2"));
    return productionFrontier;
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
