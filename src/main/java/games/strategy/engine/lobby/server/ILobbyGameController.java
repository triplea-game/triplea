package games.strategy.engine.lobby.server;

import java.util.Map;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.GUID;

public interface ILobbyGameController extends IRemote {
  RemoteName GAME_CONTROLLER_REMOTE = new RemoteName(
      "games.strategy.engine.lobby.server.IGameController.GAME_CONTROLLER_REMOTE", ILobbyGameController.class);

  void postGame(GUID gameID, GameDescription description);

  void updateGame(GUID gameID, GameDescription description);

  Map<GUID, GameDescription> listGames();

  /**
   * Test if the server can connect to the game at this address. This is used to see if the client address is network
   * accessible
   * (this will not be true if the client is behind a nat or firewall that is not properly configured)
   *
   * <p>
   * This method may only be called by the node that is hosting this game.
   * </p>
   */
  String testGame(GUID gameID);
}
