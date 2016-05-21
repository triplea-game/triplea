package games.strategy.engine.chat;

import games.strategy.engine.chat.IChatController.Tag;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.net.INode;

/**
 * Chat messages occur on this channel
 */
public interface IChatChannel extends IChannelSubscribor {
  // we get the sender from MessageContext
  void chatOccured(final String message);

  void meMessageOccured(final String message);

  void slapOccured(final String playerName);

  void speakerAdded(final INode node, final Tag tag, final long version);

  void speakerRemoved(final INode node, final long version);

  void speakerTagUpdated(final INode node, final Tag tag);

  // purely here to keep connections open and stop NATs and crap from thinking that our connection is closed when it is
  // not.
  void ping();
}
