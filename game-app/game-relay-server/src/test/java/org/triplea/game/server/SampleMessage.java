package org.triplea.game.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** A websocket message with a basic string payload for use in test. */
@AllArgsConstructor
@Getter
class SampleMessage implements WebSocketMessage {
  static final MessageType<SampleMessage> TYPE = MessageType.of(SampleMessage.class);

  private final String contents;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
