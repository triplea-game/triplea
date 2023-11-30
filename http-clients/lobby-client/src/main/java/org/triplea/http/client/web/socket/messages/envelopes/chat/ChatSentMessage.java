package org.triplea.http.client.web.socket.messages.envelopes.chat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** A chat message originating from a player sent to the lobby. */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ChatSentMessage implements WebSocketMessage {
  public static final MessageType<ChatSentMessage> TYPE = MessageType.of(ChatSentMessage.class);

  private final String chatMessage;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
