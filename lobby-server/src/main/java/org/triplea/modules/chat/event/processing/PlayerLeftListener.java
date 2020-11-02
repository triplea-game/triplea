package org.triplea.modules.chat.event.processing;

import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerLeftMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessagingBus;
import org.triplea.web.socket.WebSocketSession;

@AllArgsConstructor
public class PlayerLeftListener implements BiConsumer<WebSocketMessagingBus, WebSocketSession> {

  private final Chatters chatters;

  @Override
  public void accept(
      final WebSocketMessagingBus webSocketMessagingBus, final WebSocketSession session) {
    chatters
        .playerLeft(session)
        .ifPresent(
            playerName -> {
              if (!chatters.isPlayerConnected(playerName)) {
                webSocketMessagingBus.broadcastMessage(new PlayerLeftMessage(playerName));
              }
            });
  }
}
