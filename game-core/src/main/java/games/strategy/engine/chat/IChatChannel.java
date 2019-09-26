package games.strategy.engine.chat;

import games.strategy.engine.chat.IChatController.Tag;
import games.strategy.engine.lobby.PlayerName;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.net.INode;

/**
 * Chat messages occur on this channel.
 *
 * <p>RMI warning: the ordering of methods cannot be changed, these methods will be invoked by
 * method order number
 */
public interface IChatChannel extends IChannelSubscriber {
  // we get the sender from MessageContext
  void chatOccurred(String message);

  void slapOccurred(PlayerName playerName);

  void speakerAdded(INode node, Tag tag, long version);

  void speakerRemoved(INode node, long version);

  // purely here to keep connections open and stop NATs and crap from thinking that our connection
  // is closed when it is
  // not.
  void ping();
}
