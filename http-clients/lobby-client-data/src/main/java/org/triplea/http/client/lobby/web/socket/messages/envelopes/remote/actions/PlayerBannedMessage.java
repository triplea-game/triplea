package org.triplea.http.client.lobby.web.socket.messages.envelopes.remote.actions;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class PlayerBannedMessage implements WebSocketMessage {
  public static final MessageType<PlayerBannedMessage> TYPE =
      MessageType.of(PlayerBannedMessage.class);

  private final String ipAddress;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
