package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.net.INode;
import java.util.Map;

/**
 * A callback remote. Allows the server to add the player as an observer when the game is in
 * progress.
 */
public interface IObserverWaitingToJoin extends IRemote {
  /**
   * This method should not return until the client is ready to start the game. This includes the
   * display running, with all remote and channel listeners set up.
   */
  @RemoteActionCode(1)
  void joinGame(byte[] gameData, Map<String, INode> players);

  /** You could not join the game, usually this is due to an error. */
  @RemoteActionCode(0)
  void cannotJoinGame(String reason);
}
