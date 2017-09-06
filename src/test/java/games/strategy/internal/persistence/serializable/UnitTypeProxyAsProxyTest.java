package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.UnitType;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class UnitTypeProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<UnitType> {
  public UnitTypeProxyAsProxyTest() {
    super(UnitType.class);
  }

  @Override
  protected UnitType newGameDataComponent(final GameData gameData) {
    final UnitType unitType = new UnitType("unitType", gameData);
    initializeAttachable(unitType);
    return unitType;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.UNIT_TYPE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(UnitTypeProxy.FACTORY);
  }
}
