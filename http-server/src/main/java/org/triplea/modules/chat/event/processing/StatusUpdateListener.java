package org.triplea.modules.chat.event.processing;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerStatusUpdateReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerStatusUpdateSentMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessageContext;

@AllArgsConstructor
public class StatusUpdateListener
    implements Consumer<WebSocketMessageContext<PlayerStatusUpdateSentMessage>> {

  private final Chatters chatters;

  @Override
  public void accept(final WebSocketMessageContext<PlayerStatusUpdateSentMessage> messageContext) {
    chatters
        .lookupPlayerBySession(messageContext.getSenderSession())
        .ifPresent(
            chatParticipant -> broadcastStatusUpdateMessage(messageContext, chatParticipant));
  }

  private static void broadcastStatusUpdateMessage(
      final WebSocketMessageContext<PlayerStatusUpdateSentMessage> messageContext,
      final ChatParticipant chatParticipant) {

    messageContext
        .getMessagingBus()
        .broadcastMessage(
            new PlayerStatusUpdateReceivedMessage(
                chatParticipant.getUserName(), messageContext.getMessage().getStatus()));
  }
}
