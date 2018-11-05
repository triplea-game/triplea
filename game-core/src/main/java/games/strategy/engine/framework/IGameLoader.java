package games.strategy.engine.framework;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.player.IGamePlayer;

/**
 * A game loader is responsible for telling the framework
 * what types of players are available, for creating players, and
 * starting the game.
 * The name is somewhat misleading since it doesn't actually load the
 * game data, merely performs the game specific steps for starting the game
 * and meta data needed by the engine.
 */
public interface IGameLoader extends Serializable {
  /**
   * Create the players. Given a map of playerName -> type,
   * where type is one of the Strings returned by a getServerPlayerTypes() or PlayerType.CLIENT_PLAYER.
   *
   * @return a Set of GamePlayers
   */
  Set<IGamePlayer> createPlayers(Map<String, PlayerType> players);

  /**
   * The game is about to start.
   */
  void startGame(IGame game, Set<IGamePlayer> players, boolean headless, @Nullable Chat chat) throws Exception;

  /**
   * Get the type of the display.
   *
   * @return an interface that extends IChannelSubscribor
   */
  Class<? extends IChannelSubscribor> getDisplayType();

  Class<? extends IChannelSubscribor> getSoundType();

  /**
   * Get the type of the GamePlayer.
   *
   * <p>
   * The type must extend IRemote, and is to be used by an IRemoteManager to allow a player to be contacted remotely
   * </p>
   */
  Class<? extends IRemote> getRemotePlayerType();

  void shutDown();

  Unit createUnit(UnitType type, PlayerID owner, GameData data);
}
