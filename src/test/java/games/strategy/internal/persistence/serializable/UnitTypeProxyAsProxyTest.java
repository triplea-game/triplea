package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newUnitType;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.UnitType;

public final class UnitTypeProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<UnitType> {
  public UnitTypeProxyAsProxyTest() {
    super(UnitType.class);
  }

  @Override
  protected Collection<UnitType> createPrincipals() {
    return Arrays.asList(newUnitType(getGameData(), "unitType"));
  }
}
