package games.strategy.engine.framework;

import java.util.Collections;
import java.util.Set;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IGamePlayer;
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
    if (id == null) {
      return false;
    }
    for (final IGamePlayer gamePlayer : localPlayers) {
      if (gamePlayer.getPlayerId().equals(id) && AbstractHumanPlayer.class.isAssignableFrom(gamePlayer.getClass())) {
        return true;
      }
    }
    return false;
  }
}
