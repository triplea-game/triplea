package org.triplea.modules.chat.event.processing;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatSentMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessageContext;

@AllArgsConstructor
public class ChatMessageListener implements Consumer<WebSocketMessageContext<ChatSentMessage>> {

  private final Chatters chatters;

  @Override
  public void accept(final WebSocketMessageContext<ChatSentMessage> messageContext) {
    chatters
        .lookupPlayerBySession(messageContext.getSenderSession())
        .ifPresent(sender -> broadcastChatMessage(messageContext, sender));
  }

  private static void broadcastChatMessage(
      final WebSocketMessageContext<ChatSentMessage> messageContext, final ChatParticipant sender) {

    messageContext
        .getMessagingBus()
        .broadcastMessage(
            new ChatReceivedMessage(
                sender.getUserName(), messageContext.getMessage().getChatMessage()));
  }
}
