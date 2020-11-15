package org.triplea.http.client.web.socket.messages.envelopes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@Getter
@AllArgsConstructor
public class ServerErrorMessage implements WebSocketMessage {
  public static final MessageType<ServerErrorMessage> TYPE =
      MessageType.of(ServerErrorMessage.class);

  private final String error;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
