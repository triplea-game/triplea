package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newRepairRule;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.RepairFrontier;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

public final class RepairFrontierProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<RepairFrontier> {
  public RepairFrontierProxyAsProxyTest() {
    super(RepairFrontier.class);
  }

  @Override
  protected RepairFrontier newGameDataComponent(final GameData gameData) {
    return new RepairFrontier("repairFrontier", gameData, Arrays.asList(
        newRepairRule(gameData, "repairRule1"),
        newRepairRule(gameData, "repairRule2")));
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        CoreEqualityComparators.INTEGER_MAP,
        EngineDataEqualityComparators.REPAIR_FRONTIER,
        EngineDataEqualityComparators.REPAIR_RULE,
        EngineDataEqualityComparators.RESOURCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        IntegerMapProxy.FACTORY,
        RepairFrontierProxy.FACTORY,
        RepairRuleProxy.FACTORY,
        ResourceProxy.FACTORY);
  }
}
