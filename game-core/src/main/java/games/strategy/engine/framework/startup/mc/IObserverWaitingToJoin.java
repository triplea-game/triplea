package games.strategy.engine.framework.startup.mc;

import java.util.Map;

import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;

/**
 * A callback remote.
 * Allows the server to add the player as an observer when the game is in progress.
 */
public interface IObserverWaitingToJoin extends IRemote {
  /**
   * This method should not return until the client is ready to start the game.
   * This includes the display running, with all remote and channel listeners set up.
   */
  void joinGame(byte[] gameData, Map<String, INode> players);

  /**
   * You could not join the game, usually this is due to an error.
   */
  void cannotJoinGame(String reason);
}
