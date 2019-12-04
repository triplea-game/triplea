package org.triplea.server.lobby.chat.event.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeType;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;
import org.triplea.http.client.web.socket.messages.ClientMessageEnvelope;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/**
 * Handles processing logic when receiving chat messages and retains state of the currently
 * connected chatters. The class is responsible for receiving client messages, updating local
 * chatter state, and returns a list of server responses to broadcast or send back to users.
 */
@Slf4j
@AllArgsConstructor
public class ChatEventProcessor {

  private final Chatters chatters;

  public List<ServerResponse> processAndComputeServerResponses(
      final Session session,
      final ChatParticipant sender,
      final ClientMessageEnvelope clientMessageEnvelope) {

    final Optional<ChatClientEnvelopeType> chatClientEnvelopeType =
        parseType(clientMessageEnvelope);

    if (chatClientEnvelopeType.isEmpty()) {
      return List.of();
    }

    switch (chatClientEnvelopeType.get()) {
      case CONNECT:
        chatters.put(session, sender);
        return List.of(
            ServerResponse.backToClient(
                ChatServerEnvelopeFactory.newPlayerListing(
                    new ArrayList<>(chatters.getAllParticipants()))),
            ServerResponse.broadcast(ChatServerEnvelopeFactory.newPlayerJoined(sender)));
      case SLAP:
        final PlayerName slapped = PlayerName.of(clientMessageEnvelope.getPayload());
        return List.of(
            ServerResponse.broadcast(
                ChatServerEnvelopeFactory.newSlap(
                    PlayerSlapped.builder()
                        .slapper(sender.getPlayerName())
                        .slapped(slapped)
                        .build())));
      case MESSAGE:
        return List.of(
            ServerResponse.broadcast(
                ChatServerEnvelopeFactory.newChatMessage(
                    new ChatMessage(sender.getPlayerName(), clientMessageEnvelope.getPayload()))));
      case UPDATE_MY_STATUS:
        return List.of(
            ServerResponse.broadcast(
                ChatServerEnvelopeFactory.newStatusUpdate(
                    new StatusUpdate(sender.getPlayerName(), clientMessageEnvelope.getPayload()))));
      default:
        throw new UnsupportedOperationException(
            "Unhandled message type: " + chatClientEnvelopeType);
    }
  }

  private Optional<ChatClientEnvelopeType> parseType(
      final ClientMessageEnvelope clientMessageEnvelope) {
    try {
      return Optional.of(ChatClientEnvelopeType.valueOf(clientMessageEnvelope.getMessageType()));
    } catch (final IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  public Optional<ServerMessageEnvelope> disconnect(final Session session) {
    return chatters.removeSession(session).map(ChatServerEnvelopeFactory::newPlayerLeft);
  }

  public ServerMessageEnvelope createErrorMessage() {
    return ChatServerEnvelopeFactory.newErrorMessage();
  }
}
