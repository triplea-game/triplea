package games.strategy.engine.framework;

import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;

/**
 * A game loader is responsible for telling the framework
 * what types of players are available, for creating players, and
 * starting the game.
 * The name is somewhat misleading since it doesnt actually load the
 * game data, merely performs the game specific steps for starting the game
 * and meta data needed by the engine.
 */
public interface IGameLoader extends java.io.Serializable {
  String CLIENT_PLAYER_TYPE = "Client";

  /**
   * Return an array of player types that can play on the server.
   */
  String[] getServerPlayerTypes();

  /**
   * Create the players. Given a map of playerName -> type,
   * where type is one of the Strings returned by a getServerPlayerTypes() or IGameLoader.CLIENT_PLAYER_TYPE.
   *
   * @return a Set of GamePlayers
   */
  Set<IGamePlayer> createPlayers(Map<String, String> players);

  /**
   * The game is about to start. 
   * 
   * @throws Excpetion
   */
  void startGame(IGame game, Set<IGamePlayer> players, boolean headless) throws Exception;

  /**
   * Get the type of the display
   *
   * @return an interface that extends IChannelSubscrobor
   */
  Class<? extends IChannelSubscribor> getDisplayType();

  Class<? extends IChannelSubscribor> getSoundType();

  /**
   * Get the type of the GamePlayer.
   * <p>
   * The type must extend IRemote, and is to be used by an IRemoteManager to allow a player to be contacted remotately
   */
  Class<? extends IRemote> getRemotePlayerType();

  void shutDown();

  /**
   * A game may use a subclass of Unit to allow associating data with a particular unit. The
   * game does this by specifying a IUnitFactory that should be used to create units.
   * <p>
   * Games that do not want to subclasses of units should simply return a DefaultUnitFactory.
   * <p>
   */
  IUnitFactory getUnitFactory();
}
