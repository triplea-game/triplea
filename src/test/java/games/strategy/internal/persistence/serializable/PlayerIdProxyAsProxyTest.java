package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newPlayerId;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

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
    return Arrays.asList(EngineDataEqualityComparators.PLAYER_ID);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(PlayerIdProxy.FACTORY);
  }
}
