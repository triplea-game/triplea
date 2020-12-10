package org.triplea.http.client.web.socket.messages.envelopes.chat;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@AllArgsConstructor
@EqualsAndHashCode
public class PlayerStatusUpdateSentMessage implements WebSocketMessage {
  public static final MessageType<PlayerStatusUpdateSentMessage> TYPE =
      MessageType.of(PlayerStatusUpdateSentMessage.class);

  private final String status;

  public String getStatus() {
    return Optional.ofNullable(status).orElse("");
  }

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
