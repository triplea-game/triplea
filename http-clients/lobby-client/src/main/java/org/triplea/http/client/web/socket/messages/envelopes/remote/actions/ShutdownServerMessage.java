package org.triplea.http.client.web.socket.messages.envelopes.remote.actions;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ShutdownServerMessage implements WebSocketMessage {
  public static final MessageType<ShutdownServerMessage> TYPE =
      MessageType.of(ShutdownServerMessage.class);

  private final String gameId;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
