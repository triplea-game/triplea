package org.triplea.modules.chat.event.processing;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerResponse {
  private final boolean broadcast;
  private final ServerMessageEnvelope serverEventEnvelope;

  public static ServerResponse broadcast(final ServerMessageEnvelope serverEventEnvelope) {
    return new ServerResponse(true, serverEventEnvelope);
  }

  public static ServerResponse backToClient(final ServerMessageEnvelope serverEventEnvelope) {
    return new ServerResponse(false, serverEventEnvelope);
  }
}
