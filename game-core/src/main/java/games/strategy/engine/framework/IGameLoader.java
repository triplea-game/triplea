package games.strategy.engine.framework;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.player.IGamePlayer;

/**
 * A game loader is responsible for telling the framework what types of players are available, for creating players, and
 * starting the game. The name is somewhat misleading since it doesn't actually load the game data, merely performs the
 * game specific steps for starting the game and meta data needed by the engine.
 */
public interface IGameLoader extends Serializable {
  /**
   * Create the players. Given a map of playerName -> type,
   * where type is one of the Strings returned by a getServerPlayerTypes() or PlayerType.CLIENT_PLAYER.
   *
   * @return a Set of GamePlayers
   */
  Set<IGamePlayer> newPlayers(Map<String, PlayerType> players);

  /**
   * The game is about to start.
   */
  void startGame(IGame game, Set<IGamePlayer> players, LaunchAction launchAction, @Nullable Chat chat);

  /**
   * Get the type of the display.
   *
   * @return an interface that extends IChannelSubscriber
   */
  Class<? extends IChannelSubscriber> getDisplayType();

  Class<? extends IChannelSubscriber> getSoundType();

  /**
   * Get the type of the GamePlayer.
   *
   * <p>
   * The type must extend IRemote, and is to be used by an IRemoteManager to allow a player to be contacted remotely
   * </p>
   */
  Class<? extends IRemote> getRemotePlayerType();

  void shutDown();

  Unit newUnit(UnitType type, PlayerId owner, GameData data);
}
