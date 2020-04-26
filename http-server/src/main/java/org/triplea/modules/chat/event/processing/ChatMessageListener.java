package org.triplea.modules.chat.event.processing;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.chat.history.LobbyChatHistoryDao;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatSentMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.modules.chat.Chatters.ChatterSession;
import org.triplea.web.socket.WebSocketMessageContext;

@Builder
@Slf4j
public class ChatMessageListener implements Consumer<WebSocketMessageContext<ChatSentMessage>> {

  @Nonnull private final Chatters chatters;
  @Nonnull private final LobbyChatHistoryDao lobbyChatHistoryDao;

  public static ChatMessageListener build(final Chatters chatters, final Jdbi jdbi) {
    return ChatMessageListener.builder()
        .chatters(chatters)
        .lobbyChatHistoryDao(jdbi.onDemand(LobbyChatHistoryDao.class))
        .build();
  }

  @Override
  public void accept(final WebSocketMessageContext<ChatSentMessage> messageContext) {
    chatters
        .lookupPlayerBySession(messageContext.getSenderSession())
        .ifPresent(session -> recordAndSendMessage(session, messageContext));
  }

  private void recordAndSendMessage(
      final ChatterSession session, final WebSocketMessageContext<ChatSentMessage> messageContext) {
    final var chatReceivedMessage =
        convertMessage(session.getChatParticipant(), messageContext.getMessage().getChatMessage());
    recordInHistory(chatReceivedMessage, session);
    messageContext.broadcastMessage(chatReceivedMessage);
  }

  private static ChatReceivedMessage convertMessage(
      final ChatParticipant sender, final String message) {
    return new ChatReceivedMessage(sender.getUserName(), message);
  }

  private void recordInHistory(
      final ChatReceivedMessage chatReceivedMessage, final ChatterSession session) {
    CompletableFuture.runAsync(
            () -> lobbyChatHistoryDao.recordMessage(chatReceivedMessage, session.getApiKeyId()))
        .exceptionally(
            e -> {
              log.error("Error recording chat message in database history table", e);
              return null;
            });
  }
}
