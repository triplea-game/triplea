package games.strategy.engine.chat;

import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;

/**
 * A channel through which a chat participant's status is communicated.
 */
public interface IStatusChannel extends IChannelSubscribor {
  RemoteName STATUS_CHANNEL =
      new RemoteName(IStatusChannel.class.getName() + ".STATUS", IStatusChannel.class);

  void statusChanged(INode node, String status);
}
