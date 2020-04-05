package org.triplea.modules.chat.event.processing;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerSlapReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerSlapSentMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessageContext;

@AllArgsConstructor
public class SlapListener implements Consumer<WebSocketMessageContext<PlayerSlapSentMessage>> {
  private final Chatters chatters;

  @Override
  public void accept(final WebSocketMessageContext<PlayerSlapSentMessage> messageContext) {
    chatters
        .lookupPlayerBySession(messageContext.getSenderSession())
        .ifPresent(chatParticipant -> broadCastSlapMessage(messageContext, chatParticipant));
  }

  private static void broadCastSlapMessage(
      final WebSocketMessageContext<PlayerSlapSentMessage> messageContext,
      final ChatParticipant slapper) {

    messageContext.broadcastMessage(
        PlayerSlapReceivedMessage.builder()
            .slappingPlayer(slapper.getUserName().getValue())
            .slappedPlayer(messageContext.getMessage().getSlappedPlayer())
            .build());
  }
}
