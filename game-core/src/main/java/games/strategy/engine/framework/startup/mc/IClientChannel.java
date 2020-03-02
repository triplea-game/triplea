package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import java.util.Map;

/**
 * A channel representing a remote client of a network game. Used by the server to notify clients of
 * various events.
 */
public interface IClientChannel extends IChannelSubscriber {
  RemoteName CHANNEL_NAME =
      new RemoteName(
          "games.strategy.engine.framework.ui.IClientChannel.CHANNEL", IClientChannel.class);

@RemoteActionCode(2)
  void playerListingChanged(PlayerListing listing);

  /**
   * Invoked when all players have been selected. This event indicates the game is ready to start.
   *
   * @param players who is playing who.
   */
  void doneSelectingPlayers(byte[] gameData, Map<String, INode> players);

@RemoteActionCode(1)
  void gameReset();
}
