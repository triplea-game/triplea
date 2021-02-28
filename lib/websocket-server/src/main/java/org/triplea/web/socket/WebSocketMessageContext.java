package org.triplea.web.socket;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@Builder
@Getter
public class WebSocketMessageContext<T extends WebSocketMessage> {
  @Nonnull WebSocketMessagingBus messagingBus;
  @Nonnull WebSocketSession senderSession;
  @Nonnull T message;

  public <X extends WebSocketMessage> void sendResponse(final X responseMessage) {
    messagingBus.sendResponse(senderSession, responseMessage);
  }

  public <X extends WebSocketMessage> void broadcastMessage(final X broadcastMessage) {
    messagingBus.broadcastMessage(broadcastMessage);
  }
}
