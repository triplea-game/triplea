package org.triplea.lobby.common;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;
import java.util.Map;
import java.util.UUID;

/** A service that provides management operations for lobby games. */
public interface ILobbyGameController extends IRemote {
  RemoteName REMOTE_NAME =
      new RemoteName(
          "games.strategy.engine.lobby.server.IGameController.GAME_CONTROLLER_REMOTE",
          ILobbyGameController.class);

  void postGame(UUID gameId, GameDescription description);

  void updateGame(UUID gameId, GameDescription description);

  Map<UUID, GameDescription> listGames();
}
