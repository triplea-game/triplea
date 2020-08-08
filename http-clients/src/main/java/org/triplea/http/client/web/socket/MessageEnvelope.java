package org.triplea.http.client.web.socket;

import com.google.gson.Gson;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** Generic message carrier over websocket. Payload is serialized as JSON text. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class MessageEnvelope {
  private static final Gson gson = new Gson();

  @Getter @Nonnull private final String messageTypeId;
  /**
   * Payload itself is a JSON string. This is so we can preserve any underlying data objects. If we
   * try to store this as a generic object, then we'd have to know the generic type when
   * de-serializing from String.
   */
  @Nonnull private final String payload;

  public static <T extends WebSocketMessage> MessageEnvelope packageMessage(
      final MessageType<T> messageType, final T data) {
    return new MessageEnvelope(messageType.getMessageTypeId(), gson.toJson(data));
  }

  public <T> T getPayload(final Class<T> type) {
    return gson.fromJson(payload, type);
  }

  public boolean messageTypeIs(final MessageType<?> messageType) {
    return messageType.getMessageTypeId().equals(this.messageTypeId);
  }
}
