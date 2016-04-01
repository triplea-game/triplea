package games.strategy.engine.lobby.server;

import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.GUID;

public interface ILobbyGameBroadcaster extends IChannelSubscribor {
  public static final RemoteName GAME_BROADCASTER_CHANNEL =
      new RemoteName("games.strategy.engine.lobby.server.IGameBroadcaster.CHANNEL", ILobbyGameBroadcaster.class);

  /** @deprecated Call gameUpdated instead, it will add or update */
  public void gameAdded(GUID gameId, GameDescription description);

  public void gameUpdated(GUID gameId, GameDescription description);

  public void gameRemoved(GUID gameId);
}
