package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newPlayerId;
import static games.strategy.engine.data.TestGameDataComponentFactory.newUnit;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.FakeNamedUnitHolder;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.UnitCollection;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class UnitCollectionProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<UnitCollection> {
  public UnitCollectionProxyAsProxyTest() {
    super(UnitCollection.class);
  }

  @Override
  protected UnitCollection newGameDataComponent(final GameData gameData) {
    final UnitCollection unitCollection = new UnitCollection(new FakeNamedUnitHolder("name", "type"), gameData);
    unitCollection.addAll(Arrays.asList(
        newUnit(gameData, newPlayerId(gameData, "playerId1"), "unitType1"),
        newUnit(gameData, newPlayerId(gameData, "playerId2"), "unitType2")));
    return unitCollection;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        EngineDataEqualityComparators.PLAYER_ID,
        EngineDataEqualityComparators.UNIT,
        EngineDataEqualityComparators.UNIT_COLLECTION,
        EngineDataEqualityComparators.UNIT_TYPE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        GuidProxy.FACTORY,
        PlayerIdProxy.FACTORY,
        UnitProxy.FACTORY,
        UnitCollectionProxy.FACTORY,
        UnitTypeProxy.FACTORY);
  }
}
