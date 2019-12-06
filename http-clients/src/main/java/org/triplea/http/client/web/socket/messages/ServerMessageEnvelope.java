package org.triplea.http.client.web.socket.messages;

import com.google.gson.Gson;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** A message sent from the server to client. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
public class ServerMessageEnvelope {
  private static final Gson gson = new Gson();

  @Getter @Nonnull private final String messageType;
  /**
   * Payload itself is a JSON string. This is so we can preserve any underlying data objects. If we
   * try to store this as a generic object, then we'd have to know the generic type when
   * de-serializing from String.
   */
  @Nonnull private final String payload;

  public static <T> ServerMessageEnvelope packageMessage(final String messageType, final T data) {
    return new ServerMessageEnvelope(messageType, gson.toJson(data));
  }

  public <T> T getPayload(final Class<T> type) {
    return gson.fromJson(payload, type);
  }
}
