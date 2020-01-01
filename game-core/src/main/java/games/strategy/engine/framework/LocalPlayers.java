package games.strategy.engine.framework;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.player.Player;
import games.strategy.triplea.player.AbstractHumanPlayer;
import java.util.Collections;
import java.util.Set;

/**
 * A collection of {@link Player}s that are local to the current node. For example, in a network
 * game, the human at a single node may be responsible for managing multiple game players, such as
 * all member nations of the Allies.
 */
public class LocalPlayers {
  protected final Set<Player> localPlayers;

  public LocalPlayers(final Set<Player> localPlayers) {
    this.localPlayers = localPlayers;
  }

  public Set<Player> getLocalPlayers() {
    return Collections.unmodifiableSet(localPlayers);
  }

  public boolean playing(final GamePlayer player) {
    return player != null
        && localPlayers.stream()
            .anyMatch(gamePlayer -> isGamePlayerWithPlayerId(gamePlayer, player));
  }

  private static boolean isGamePlayerWithPlayerId(
      final Player player, final GamePlayer gamePlayer) {
    return player.getGamePlayer().equals(gamePlayer)
        && AbstractHumanPlayer.class.isAssignableFrom(player.getClass());
  }
}
