package games.strategy.engine.chat;

import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteActionCode;
import java.io.Serial;
import java.io.Serializable;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.web.socket.messages.envelopes.chat.ChatParticipant;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** Chat messages occur on this channel. */
public interface IChatChannel extends IChannelSubscriber {
  // we get the sender from MessageContext
  @RemoteActionCode(0)
  void chatOccurred(String message);

  @AllArgsConstructor
  class ChatMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8069447089999124301L;

    public static final MessageType<ChatMessage> TYPE = MessageType.of(ChatMessage.class);

    @Nonnull private final String message;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(IChatChannel iChatChannel) {
      iChatChannel.chatOccurred(message);
    }
  }

  @RemoteActionCode(2)
  void slapOccurred(UserName userName);

  @AllArgsConstructor
  class SlapMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5563896137216078099L;

    public static final MessageType<SlapMessage> TYPE = MessageType.of(SlapMessage.class);

    @Nonnull private final UserName userName;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(IChatChannel iChatChannel) {
      iChatChannel.slapOccurred(userName);
    }
  }

  @RemoteActionCode(3)
  void speakerAdded(ChatParticipant chatParticipant);

  @AllArgsConstructor
  class SpeakAddedMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 2971033844411110987L;

    public static final MessageType<SpeakAddedMessage> TYPE =
        MessageType.of(SpeakAddedMessage.class);

    @Nonnull private final ChatParticipant chatParticipant;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(IChatChannel iChatChannel) {
      iChatChannel.speakerAdded(chatParticipant);
    }
  }

  @RemoteActionCode(4)
  void speakerRemoved(UserName userName);

  @AllArgsConstructor
  class SpeakerRemovedMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6114060409231051221L;

    public static final MessageType<SpeakerRemovedMessage> TYPE =
        MessageType.of(SpeakerRemovedMessage.class);

    @Nonnull private final String userName;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(IChatChannel iChatChannel) {
      iChatChannel.speakerRemoved(UserName.of(userName));
    }
  }

  // purely here to keep connections open and stop NATs and crap from thinking that our connection
  // is closed when it is not.
  @RemoteActionCode(1)
  void ping();

  @AllArgsConstructor
  class PingMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 4753221055964055443L;

    public static final MessageType<PingMessage> TYPE = MessageType.of(PingMessage.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(IChatChannel iChatChannel) {
      iChatChannel.ping();
    }
  }

  @RemoteActionCode(5)
  void statusChanged(UserName userName, String status);

  @AllArgsConstructor
  class StatusChangedMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 7238991029348871155L;

    public static final MessageType<StatusChangedMessage> TYPE =
        MessageType.of(StatusChangedMessage.class);

    private final UserName userName;
    private final String status;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(IChatChannel iChatChannel) {
      iChatChannel.statusChanged(userName, status);
    }
  }
}
