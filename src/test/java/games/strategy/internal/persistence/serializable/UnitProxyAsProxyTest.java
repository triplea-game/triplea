package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newPlayerId;
import static games.strategy.engine.data.TestGameDataComponentFactory.newUnit;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class UnitProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<Unit> {
  public UnitProxyAsProxyTest() {
    super(Unit.class);
  }

  @Override
  protected Unit newGameDataComponent(final GameData gameData) {
    return newUnit(gameData, newPlayerId(gameData, "playerId"), "unitType");
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        EngineDataEqualityComparators.PLAYER_ID,
        EngineDataEqualityComparators.UNIT,
        EngineDataEqualityComparators.UNIT_TYPE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        GuidProxy.FACTORY,
        PlayerIdProxy.FACTORY,
        UnitProxy.FACTORY,
        UnitTypeProxy.FACTORY);
  }
}
