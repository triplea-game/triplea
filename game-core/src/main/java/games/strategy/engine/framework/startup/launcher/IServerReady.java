package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;

/**
 * Allows for the server to wait for all clients to finish initialization before starting the game.
 */
public interface IServerReady extends IRemote {
  @RemoteActionCode(0)
  void clientReady();
}
