package games.strategy.engine.chat;

import games.strategy.engine.message.IChannelSubscriber;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatParticipant;

/**
 * Chat messages occur on this channel.
 *
 * <p>RMI warning: the ordering of methods cannot be changed, these methods will be invoked by
 * method order number
 */
public interface IChatChannel extends IChannelSubscriber {
  // we get the sender from MessageContext
  void chatOccurred(String message);

  void slapOccurred(UserName userName);

  void speakerAdded(ChatParticipant chatParticipant);

  void speakerRemoved(UserName userName);

  // purely here to keep connections open and stop NATs and crap from thinking that our connection
  // is closed when it is
  // not.
  void ping();

  void statusChanged(UserName userName, String status);
}
