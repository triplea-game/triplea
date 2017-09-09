package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newPlayerId;
import static games.strategy.engine.data.TestGameDataComponentFactory.newUnit;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class UnitProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<Unit> {
  private PlayerID playerId;

  public UnitProxyAsProxyTest() {
    super(Unit.class);
  }

  @Override
  protected Unit newGameDataComponent(final GameData gameData) {
    return newUnit(gameData, playerId, "unitType");
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        EngineDataEqualityComparators.UNIT,
        EngineDataEqualityComparators.UNIT_TYPE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        GuidProxy.FACTORY,
        UnitProxy.FACTORY,
        UnitTypeProxy.FACTORY);
  }

  @Override
  protected void initializeGameDataComponents(final GameData gameData) {
    playerId = newPlayerId(gameData, "playerId");
  }

  @Override
  protected void prepareDeserializedPrincipal(final Unit actual) {
    actual.setOwner(playerId);
  }
}
