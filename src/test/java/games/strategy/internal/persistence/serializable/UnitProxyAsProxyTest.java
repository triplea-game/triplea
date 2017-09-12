package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newPlayerId;
import static games.strategy.engine.data.TestGameDataComponentFactory.newUnit;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;

public final class UnitProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<Unit> {
  private PlayerID playerId;

  public UnitProxyAsProxyTest() {
    super(Unit.class);
  }

  @Override
  protected Collection<Unit> createPrincipals() {
    return Arrays.asList(newUnit(getGameData(), playerId, "unitType"));
  }

  @Override
  protected void prepareDeserializedPrincipal(final Unit actual) {
    super.prepareDeserializedPrincipal(actual);

    actual.setOwner(playerId);
  }

  @Before
  @Override
  public void setUp() {
    super.setUp();

    playerId = newPlayerId(getGameData(), "playerId");
  }
}
