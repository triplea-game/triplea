package org.triplea.http.client.web.socket;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@EqualsAndHashCode
public class ExampleMessage implements WebSocketMessage {
  public static final MessageType<ExampleMessage> TYPE = MessageType.of(ExampleMessage.class);

  @Getter private final String messageData;

  public ExampleMessage(final String messageData) {
    this.messageData = messageData;
  }

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
