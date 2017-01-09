package org.triplea.lobby.server;

import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.GUID;

public interface ILobbyGameBroadcaster extends IChannelSubscribor {
  RemoteName GAME_BROADCASTER_CHANNEL =
      new RemoteName("org.triplea.lobby.server.IGameBroadcaster.CHANNEL", ILobbyGameBroadcaster.class);

  void gameUpdated(GUID gameId, GameDescription description);

  void gameRemoved(GUID gameId);
}
