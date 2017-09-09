package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newPlayerId;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

public final class PlayerIdProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<PlayerID> {
  public PlayerIdProxyAsProxyTest() {
    super(PlayerID.class);
  }

  @Override
  protected PlayerID newGameDataComponent(final GameData gameData) {
    return newPlayerId(gameData, "playerId");
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        CoreEqualityComparators.INTEGER_MAP,
        EngineDataEqualityComparators.PLAYER_ID,
        EngineDataEqualityComparators.PRODUCTION_FRONTIER,
        EngineDataEqualityComparators.PRODUCTION_RULE,
        EngineDataEqualityComparators.REPAIR_FRONTIER,
        EngineDataEqualityComparators.REPAIR_RULE,
        EngineDataEqualityComparators.RESOURCE,
        EngineDataEqualityComparators.RESOURCE_COLLECTION,
        EngineDataEqualityComparators.TECHNOLOGY_FRONTIER_LIST,
        EngineDataEqualityComparators.UNIT,
        EngineDataEqualityComparators.UNIT_TYPE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        FakeTechAdvanceProxy.FACTORY,
        GuidProxy.FACTORY,
        IntegerMapProxy.FACTORY,
        PlayerIdProxy.FACTORY,
        ProductionFrontierProxy.FACTORY,
        ProductionRuleProxy.FACTORY,
        RepairFrontierProxy.FACTORY,
        RepairRuleProxy.FACTORY,
        ResourceProxy.FACTORY,
        TechnologyFrontierProxy.FACTORY,
        UnitProxy.FACTORY,
        UnitTypeProxy.FACTORY);
  }
}
