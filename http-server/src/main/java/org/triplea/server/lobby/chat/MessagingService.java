package org.triplea.server.lobby.chat;

import com.google.gson.JsonSyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.websocket.Session;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.events.client.ClientMessageEnvelope;
import org.triplea.http.client.lobby.chat.events.server.ServerMessageEnvelope;
import org.triplea.lobby.server.db.dao.api.key.LobbyApiKeyDaoWrapper;
import org.triplea.lobby.server.db.dao.api.key.UserWithRoleRecord;
import org.triplea.server.lobby.chat.event.processing.ChatEventProcessor;
import org.triplea.server.lobby.chat.event.processing.ServerResponse;

@Slf4j
@Builder
class MessagingService {
  @Nonnull private final LobbyApiKeyDaoWrapper apiKeyDaoWrapper;
  @Nonnull private final ChatEventProcessor chatEventProcessor;
  /** Sends to a single session. */
  @Nonnull private final BiConsumer<Session, ServerMessageEnvelope> messageSender;
  /** Sends to all connected sessions. */
  @Nonnull private final BiConsumer<Session, ServerMessageEnvelope> messageBroadcaster;

  @Nonnull private final Function<UserWithRoleRecord, ChatParticipant> chatParticipantAdapter;

  void handleMessage(final Session session, final String message) {
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
    apiKeyDaoWrapper
        .lookupByApiKey(clientEventEnvelope.getApiKey())
        .map(chatParticipantAdapter)
        .map(
            chatParticipant ->
                chatEventProcessor.process(session, chatParticipant, clientEventEnvelope))
        .ifPresentOrElse(
            responses -> sendMessages(session, responses), () -> logRequestIgnoredWarning(session));
  }

  private void sendMessages(final Session session, final List<ServerResponse> responses) {
    if (responses.isEmpty()) {
      logRequestIgnoredWarning(session);
    }

    responses.forEach(
        response -> {
          if (response.isBroadcast()) {
            messageBroadcaster.accept(session, response.getServerEventEnvelope());
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

  void handleError(final Session session, final Throwable throwable) {
    final ServerMessageEnvelope error = chatEventProcessor.createErrorMessage();
    messageSender.accept(session, error);
  }

  void handleDisconnect(final Session session) {
    chatEventProcessor
        .disconnect(session)
        .ifPresent(envelope -> messageBroadcaster.accept(session, envelope));
  }
}
