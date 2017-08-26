package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.RepairFrontier;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

public final class RepairFrontierProxyAsProxyTest extends AbstractProxyTestCase<RepairFrontier> {
  public RepairFrontierProxyAsProxyTest() {
    super(RepairFrontier.class);
  }

  @Override
  protected Collection<RepairFrontier> createPrincipals() {
    return Arrays.asList(newRepairFrontier());
  }

  private static RepairFrontier newRepairFrontier() {
    final GameData gameData = TestGameDataFactory.newValidGameData();
    return new RepairFrontier("repairFrontier", gameData, Arrays.asList(
        TestGameDataComponentFactory.newRepairRule(gameData, "repairRule1"),
        TestGameDataComponentFactory.newRepairRule(gameData, "repairRule2")));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(CoreEqualityComparators.INTEGER_MAP)
        .add(EngineDataEqualityComparators.REPAIR_FRONTIER)
        .add(EngineDataEqualityComparators.REPAIR_RULE)
        .add(EngineDataEqualityComparators.RESOURCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(IntegerMapProxy.FACTORY)
        .add(RepairFrontierProxy.FACTORY)
        .add(RepairRuleProxy.FACTORY)
        .add(ResourceProxy.FACTORY)
        .build();
  }
}
