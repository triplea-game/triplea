package org.triplea.modules.chat.event.processing;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatterListingMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ConnectToChatMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerJoinedMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessageContext;

@AllArgsConstructor
public class PlayerConnectedListener
    implements Consumer<WebSocketMessageContext<ConnectToChatMessage>> {

  private final Chatters chatters;

  @Override
  public void accept(final WebSocketMessageContext<ConnectToChatMessage> context) {
    chatters
        .connectPlayer(context.getMessage().getApiKey(), context.getSenderSession())
        .ifPresent(
            chatParticipant -> {
              context.sendResponse(new ChatterListingMessage(chatters.getChatters()));
              context.broadcastMessage(new PlayerJoinedMessage(chatParticipant));
            });
  }
}
