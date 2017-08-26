package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

public final class RepairRuleProxyAsProxyTest extends AbstractProxyTestCase<RepairRule> {
  public RepairRuleProxyAsProxyTest() {
    super(RepairRule.class);
  }

  @Override
  protected Collection<RepairRule> createPrincipals() {
    return Arrays.asList(TestGameDataComponentFactory.newRepairRule(
        TestGameDataFactory.newValidGameData(),
        "repairRule"));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(CoreEqualityComparators.INTEGER_MAP)
        .add(EngineDataEqualityComparators.REPAIR_RULE)
        .add(EngineDataEqualityComparators.RESOURCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(IntegerMapProxy.FACTORY)
        .add(RepairRuleProxy.FACTORY)
        .add(ResourceProxy.FACTORY)
        .build();
  }
}
