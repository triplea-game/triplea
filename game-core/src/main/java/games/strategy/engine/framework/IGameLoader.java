package games.strategy.engine.framework;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.player.Player;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * A game loader is responsible for telling the framework what types of players are available, for
 * creating players, and starting the game. The name is somewhat misleading since it doesn't
 * actually load the game data, merely performs the game specific steps for starting the game and
 * meta data needed by the engine.
 */
public interface IGameLoader extends Serializable {
  /**
   * Create the players. Given a map of playerName -> type, where type is one of the Strings
   * returned by a getServerPlayerTypes() or PlayerType.CLIENT_PLAYER.
   *
   * @return a Set of GamePlayers
   */
  Set<Player> newPlayers(Map<String, PlayerType> players);

  /** The game is about to start. */
  void startGame(IGame game, Set<Player> players, LaunchAction launchAction, @Nullable Chat chat);

  void shutDown();
}
