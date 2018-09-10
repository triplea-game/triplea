package org.triplea.lobby.common;

import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.GUID;

/**
 * A service that notifies nodes of lobby game state changes (e.g. when games are added to or removed from the lobby).
 */
public interface ILobbyGameBroadcaster extends IChannelSubscribor {
  RemoteName REMOTE_NAME =
      new RemoteName("games.strategy.engine.lobby.server.IGameBroadcaster.CHANNEL", ILobbyGameBroadcaster.class);

  void gameUpdated(GUID gameId, GameDescription description);

  void gameRemoved(GUID gameId);
}
