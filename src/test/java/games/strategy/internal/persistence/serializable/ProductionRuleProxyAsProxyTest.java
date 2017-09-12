package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newProductionRule;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.ProductionRule;

public final class ProductionRuleProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<ProductionRule> {
  public ProductionRuleProxyAsProxyTest() {
    super(ProductionRule.class);
  }

  @Override
  protected Collection<ProductionRule> createPrincipals() {
    return Arrays.asList(newProductionRule(getGameData(), "productionRule"));
  }
}
