package games.strategy.engine.chat;

import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;

public interface IStatusChannel extends IChannelSubscribor {
  public static final RemoteName STATUS_CHANNEL =
      new RemoteName("games.strategy.engine.chat.IStatusChannel.STATUS", IStatusChannel.class);

  public void statusChanged(INode node, String status);
}
