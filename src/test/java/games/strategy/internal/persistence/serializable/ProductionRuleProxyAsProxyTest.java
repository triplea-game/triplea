package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;

public final class ProductionRuleProxyAsProxyTest extends AbstractProxyTestCase<ProductionRule> {
  public ProductionRuleProxyAsProxyTest() {
    super(ProductionRule.class);
  }

  @Override
  protected void assertPrincipalEquals(final ProductionRule expected, final ProductionRule actual) {
    assertThat(actual, is(equalTo(expected)));
  }

  @Override
  protected Collection<ProductionRule> createPrincipals() {
    return Arrays.asList(TestGameDataComponentFactory.newProductionRule(
        TestGameDataFactory.newValidGameData(),
        "productionRule"));
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
