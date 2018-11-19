package games.strategy.engine.framework.startup.mc;

import java.util.Map;

import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;

/**
 * A channel representing a remote client of a network game. Used by the server to notify clients of various events.
 */
public interface IClientChannel extends IChannelSubscribor {
  RemoteName CHANNEL_NAME =
      new RemoteName("games.strategy.engine.framework.ui.IClientChannel.CHANNEL", IClientChannel.class);

  void playerListingChanged(PlayerListing listing);

  /**
   * Invoked when all players have been selected. This event indicates the game is ready to start.
   *
   * @param players who is playing who.
   */
  void doneSelectingPlayers(byte[] gameData, Map<String, INode> players);

  void gameReset();
}
