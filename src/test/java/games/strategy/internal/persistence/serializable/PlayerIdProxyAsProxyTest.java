package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newPlayerId;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.PlayerID;

public final class PlayerIdProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<PlayerID> {
  public PlayerIdProxyAsProxyTest() {
    super(PlayerID.class);
  }

  @Override
  protected Collection<PlayerID> createPrincipals() {
    return Arrays.asList(newPlayerId(getGameData(), "playerId"));
  }
}
