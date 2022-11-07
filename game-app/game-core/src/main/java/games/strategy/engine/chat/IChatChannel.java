package games.strategy.engine.chat;

import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteActionCode;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** Chat messages occur on this channel. */
public interface IChatChannel extends IChannelSubscriber {
  // we get the sender from MessageContext
  @RemoteActionCode(0)
  void chatOccurred(String message);

  @AllArgsConstructor
  class ChatMessage implements WebSocketMessage {
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
  class SlapMessage implements WebSocketMessage {
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
  class SpeakAddedMessage implements WebSocketMessage {
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
  class SpeakerRemovedMessage implements WebSocketMessage {
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
  class PingMessage implements WebSocketMessage {
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
  class StatusChangedMessage implements WebSocketMessage {
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
