package org.triplea.http.client.lobby.web.socket.messages.envelopes.chat;

import javax.annotation.Nonnull;
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
public class PlayerSlapReceivedMessage implements WebSocketMessage {
  public static final MessageType<PlayerSlapReceivedMessage> TYPE =
      MessageType.of(PlayerSlapReceivedMessage.class);

  @Nonnull private final String slappedPlayer;
  @Nonnull private final String slappingPlayer;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
