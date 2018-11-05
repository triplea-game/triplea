package games.strategy.engine.framework;

import java.util.Collections;
import java.util.Set;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.player.IGamePlayer;
import games.strategy.triplea.player.AbstractHumanPlayer;

public class LocalPlayers {
  protected final Set<IGamePlayer> localPlayers;

  public LocalPlayers(final Set<IGamePlayer> localPlayers) {
    this.localPlayers = localPlayers;
  }

  public Set<IGamePlayer> getLocalPlayers() {
    return Collections.unmodifiableSet(localPlayers);
  }

  public boolean playing(final PlayerID id) {
    return id != null && localPlayers.stream()
        .anyMatch(gamePlayer -> isGamePlayerWithPlayerId(gamePlayer, id));
  }

  private static boolean isGamePlayerWithPlayerId(
      final IGamePlayer gamePlayer,
      final PlayerID id) {
    return gamePlayer.getPlayerId().equals(id)
        && AbstractHumanPlayer.class.isAssignableFrom(gamePlayer.getClass());
  }
}
