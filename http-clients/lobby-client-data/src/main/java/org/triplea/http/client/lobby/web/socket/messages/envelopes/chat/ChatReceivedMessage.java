package org.triplea.http.client.lobby.web.socket.messages.envelopes.chat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * A chat message that is being broadcast to all players, this is a message that was received and
 * then is being relayed to all players.
 */
@EqualsAndHashCode
@AllArgsConstructor
@Getter
public class ChatReceivedMessage implements WebSocketMessage {
  public static final MessageType<ChatReceivedMessage> TYPE =
      MessageType.of(ChatReceivedMessage.class);
  public static final int MAX_MESSAGE_LENGTH = 240;

  private final String sender;
  @Getter private final String message;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
