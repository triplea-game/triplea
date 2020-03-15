package org.triplea.server.lobby.chat.event.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.websocket.Session;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeType;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerEnvelopeFactory;
import org.triplea.http.client.lobby.chat.messages.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;
import org.triplea.http.client.web.socket.messages.ClientMessageEnvelope;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.server.http.web.socket.SessionSet;

/**
 * Handles processing logic when receiving chat messages and retains state of the currently
 * connected chatters. The class is responsible for receiving client messages, updating local
 * chatter state, and returns a list of server responses to broadcast or send back to users.
 */
@AllArgsConstructor
public class ChatEventProcessor {

  private final Chatters chatters;
  private final SessionSet sessionSet;

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
        sessionSet.put(session);
        chatters.put(session, sender);
        return playerConnectedMessages(sender);
      case SLAP:
        return List.of(playerSlappedMessage(sender, clientMessageEnvelope));
      case MESSAGE:
        return List.of(chatMessage(sender, clientMessageEnvelope));
      case UPDATE_MY_STATUS:
        return List.of(statusUpdateMessage(sender, clientMessageEnvelope));
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

  private List<ServerResponse> playerConnectedMessages(final ChatParticipant sender) {
    return List.of(
        ServerResponse.backToClient(
            ChatServerEnvelopeFactory.newPlayerListing(
                new ArrayList<>(chatters.getAllParticipants()))),
        ServerResponse.broadcast(ChatServerEnvelopeFactory.newPlayerJoined(sender)));
  }

  private ServerResponse playerSlappedMessage(
      final ChatParticipant sender, final ClientMessageEnvelope clientMessageEnvelope) {
    final UserName slapped = UserName.of(clientMessageEnvelope.getPayload());
    return ServerResponse.broadcast(
        ChatServerEnvelopeFactory.newSlap(
            PlayerSlapped.builder().slapper(sender.getUserName()).slapped(slapped).build()));
  }

  private ServerResponse chatMessage(
      final ChatParticipant sender, final ClientMessageEnvelope clientMessageEnvelope) {
    return ServerResponse.broadcast(
        ChatServerEnvelopeFactory.newChatMessage(
            new ChatMessage(sender.getUserName(), clientMessageEnvelope.getPayload())));
  }

  private ServerResponse statusUpdateMessage(
      final ChatParticipant sender, final ClientMessageEnvelope clientMessageEnvelope) {
    return ServerResponse.broadcast(
        ChatServerEnvelopeFactory.newStatusUpdate(
            new StatusUpdate(sender.getUserName(), clientMessageEnvelope.getPayload())));
  }

  public Optional<ServerMessageEnvelope> disconnect(final Session session) {
    return chatters.removeSession(session).map(ChatServerEnvelopeFactory::newPlayerLeft);
  }

  public ServerMessageEnvelope createErrorMessage() {
    return ChatServerEnvelopeFactory.newErrorMessage();
  }
}
