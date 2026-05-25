package org.triplea.http.client.web.socket.messages;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode
public class MessageType<T extends WebSocketMessage> {
  String messageTypeId;
  Class<T> payloadType;

  public static <X extends WebSocketMessage> MessageType<X> of(final Class<X> classType) {
    return new MessageType<>(classType.getName(), classType);
  }
}
