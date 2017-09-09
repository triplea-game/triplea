package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newRepairRule;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.RepairRule;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

public final class RepairRuleProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<RepairRule> {
  public RepairRuleProxyAsProxyTest() {
    super(RepairRule.class);
  }

  @Override
  protected RepairRule newGameDataComponent(final GameData gameData) {
    return newRepairRule(gameData, "repairRule");
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        CoreEqualityComparators.INTEGER_MAP,
        EngineDataEqualityComparators.REPAIR_RULE,
        EngineDataEqualityComparators.RESOURCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        IntegerMapProxy.FACTORY,
        RepairRuleProxy.FACTORY,
        ResourceProxy.FACTORY);
  }
}
