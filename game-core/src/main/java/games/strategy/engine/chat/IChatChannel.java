package games.strategy.engine.chat;

import games.strategy.engine.chat.IChatController.Tag;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.net.INode;

/**
 * Chat messages occur on this channel.
 * <p>
 * RMI warning: the ordering of methods cannot be changed, these methods will be invoked by method order number
 * </p>
 */
public interface IChatChannel extends IChannelSubscriber {
  // we get the sender from MessageContext
  void chatOccured(String message);

  void meMessageOccured(String message);

  void slapOccured(String playerName);

  void speakerAdded(INode node, Tag tag, long version);

  void speakerRemoved(INode node, long version);

  void speakerTagUpdated(INode node, Tag tag);

  // purely here to keep connections open and stop NATs and crap from thinking that our connection is closed when it is
  // not.
  void ping();
}
