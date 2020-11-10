package org.triplea.http.client.web.socket.messages.envelopes.chat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** This is an info message that is sent to all players, eg: "Player has left". */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ChatEventReceivedMessage implements WebSocketMessage {

  public static final MessageType<ChatEventReceivedMessage> TYPE =
      MessageType.of(ChatEventReceivedMessage.class);

  private final String message;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
