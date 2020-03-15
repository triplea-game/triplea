package games.strategy.engine.chat;

import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteActionCode;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;

/** Chat messages occur on this channel. */
public interface IChatChannel extends IChannelSubscriber {
  // we get the sender from MessageContext
  @RemoteActionCode(0)
  void chatOccurred(String message);

  @RemoteActionCode(2)
  void slapOccurred(UserName userName);

  @RemoteActionCode(3)
  void speakerAdded(ChatParticipant chatParticipant);

  @RemoteActionCode(4)
  void speakerRemoved(UserName userName);

  // purely here to keep connections open and stop NATs and crap from thinking that our connection
  // is closed when it is
  // not.
  @RemoteActionCode(1)
  void ping();

  @RemoteActionCode(5)
  void statusChanged(UserName userName, String status);
}
