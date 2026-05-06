package org.triplea.http.client.lobby.web.socket.messages.envelopes.chat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class PlayerLeftMessage implements WebSocketMessage {
  public static final MessageType<PlayerLeftMessage> TYPE = MessageType.of(PlayerLeftMessage.class);

  private final String userName;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
