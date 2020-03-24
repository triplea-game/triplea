package org.triplea.modules.chat;

import com.google.gson.JsonSyntaxException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.websocket.Session;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.UserWithRoleRecord;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.messages.ClientMessageEnvelope;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.modules.chat.event.processing.ChatEventProcessor;
import org.triplea.modules.chat.event.processing.ServerResponse;

@Slf4j
@Builder
public class ChatMessagingService {
  @Nonnull private final ApiKeyDaoWrapper apiKeyDaoWrapper;
  @Nonnull private final ChatEventProcessor chatEventProcessor;
  /** Sends to a single session. */
  @Nonnull private final BiConsumer<Session, ServerMessageEnvelope> messageSender;
  /** Sends to all connected sessions. */
  @Nonnull private final BiConsumer<Collection<Session>, ServerMessageEnvelope> messageBroadcaster;

  @Nonnull private final Function<UserWithRoleRecord, ChatParticipant> chatParticipantAdapter;

  public void handleMessage(final Session session, final String message) {
    // TODO: Project#12 Bans: check API key
    deserializeFromJson(session, message)
        .ifPresent(envelope -> handleClientEnvelope(session, envelope));
  }

  private Optional<ClientMessageEnvelope> deserializeFromJson(
      final Session session, final String message) {
    try {
      return Optional.of(ClientMessageEnvelope.fromJson(message));
    } catch (final JsonSyntaxException e) {
      log.warn(
          "Ignoring badly formatted JSON request from: {}",
          InetExtractor.extract(session.getUserProperties()));
      return Optional.empty();
    }
  }

  private void handleClientEnvelope(
      final Session session, final ClientMessageEnvelope clientEventEnvelope) {
    lookupChatParticipantByApiKey(clientEventEnvelope.getApiKey())
        .map(
            chatParticipant ->
                chatEventProcessor.processAndComputeServerResponses(
                    session, chatParticipant, clientEventEnvelope))
        .ifPresentOrElse(
            responses -> sendMessages(session, responses), () -> logRequestIgnoredWarning(session));
  }

  private Optional<ChatParticipant> lookupChatParticipantByApiKey(final ApiKey apiKey) {
    return apiKeyDaoWrapper.lookupByApiKey(apiKey).map(chatParticipantAdapter);
  }

  private void sendMessages(final Session session, final Collection<ServerResponse> responses) {
    if (responses.isEmpty()) {
      logRequestIgnoredWarning(session);
    }

    responses.forEach(
        response -> {
          if (response.isBroadcast()) {
            messageBroadcaster.accept(session.getOpenSessions(), response.getServerEventEnvelope());
          } else {
            messageSender.accept(session, response.getServerEventEnvelope());
          }
        });
  }

  private void logRequestIgnoredWarning(final Session session) {
    log.warn(
        "Ignoring message from client (invalid API key or invalid client message type) from IP: "
            + InetExtractor.extract(session.getUserProperties()));
  }

  public void handleError(final Session session, final Throwable throwable) {
    log.warn(
        "Messaging service error processing request, sending error message to client", throwable);
    final ServerMessageEnvelope error = chatEventProcessor.createErrorMessage();
    messageSender.accept(session, error);
  }

  public void handleDisconnect(final Session session) {
    chatEventProcessor
        .disconnect(session)
        .ifPresent(envelope -> messageBroadcaster.accept(session.getOpenSessions(), envelope));
  }
}
