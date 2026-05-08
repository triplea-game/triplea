package org.triplea.http.client.lobby.web.socket.messages.envelopes.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Builder
public class PlayerSlapSentMessage implements WebSocketMessage {
  public static final MessageType<PlayerSlapSentMessage> TYPE =
      MessageType.of(PlayerSlapSentMessage.class);

  private final String slappedPlayer;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
