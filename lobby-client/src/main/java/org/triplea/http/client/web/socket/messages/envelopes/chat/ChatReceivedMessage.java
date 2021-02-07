package org.triplea.http.client.web.socket.messages.envelopes.chat;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.StringUtils;

/**
 * A chat message that is being broadcast to all players, this is a message that was received and
 * then is being relayed to all players.
 */
@EqualsAndHashCode
public class ChatReceivedMessage implements WebSocketMessage {
  public static final MessageType<ChatReceivedMessage> TYPE =
      MessageType.of(ChatReceivedMessage.class);
  public static final int MAX_MESSAGE_LENGTH = 240;

  private final String sender;
  @Getter private final String message;

  public ChatReceivedMessage(final UserName sender, final String message) {
    this.sender = sender.getValue();

    this.message = message == null ? "" : StringUtils.truncate(message, MAX_MESSAGE_LENGTH);
  }

  public UserName getSender() {
    return UserName.of(sender);
  }

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
