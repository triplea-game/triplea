package org.triplea.server.lobby.chat.event.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.events.client.ClientMessageEnvelope;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;
import org.triplea.http.client.lobby.chat.events.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.events.server.ServerMessageEnvelope;
import org.triplea.http.client.lobby.chat.events.server.StatusUpdate;
import org.triplea.server.lobby.chat.InetExtractor;

/**
 * Handles processing logic when receiving chat messages and retains state of the currently
 * connected chatters. The class is responsible for receiving client messages, updating local
 * chatter state, and returns a list of server responses to broadcast or send back to users.
 */
@Slf4j
@AllArgsConstructor
public class ChatEventProcessor {

  private final Chatters chatters;

  public List<ServerResponse> process(
      final Session session,
      final ChatParticipant sender,
      final ClientMessageEnvelope clientEventEnvelope) {

    final List<ServerResponse> responses = new ArrayList<>();

    switch (clientEventEnvelope.getMessageType()) {
      case CONNECT:
        chatters.put(session, sender);
        responses.add(
            ServerResponse.backToClient(
                ServerMessageEnvelopeFactory.newPlayerListing(
                    new ArrayList<>(chatters.getAllParticipants()))));
        responses.add(
            ServerResponse.broadcast(ServerMessageEnvelopeFactory.newPlayerJoined(sender)));
        return responses;
      case SLAP:
        final PlayerName slapped = PlayerName.of(clientEventEnvelope.getPayload());
        responses.add(
            ServerResponse.broadcast(
                ServerMessageEnvelopeFactory.newSlap(
                    PlayerSlapped.builder()
                        .slapper(sender.getPlayerName())
                        .slapped(slapped)
                        .build())));
        return responses;
      case MESSAGE:
        responses.add(
            ServerResponse.broadcast(
                ServerMessageEnvelopeFactory.newChatMessage(
                    new ChatMessage(sender.getPlayerName(), clientEventEnvelope.getPayload()))));
        return responses;
      case UPDATE_MY_STATUS:
        responses.add(
            ServerResponse.broadcast(
                ServerMessageEnvelopeFactory.newStatusUpdate(
                    new StatusUpdate(sender.getPlayerName(), clientEventEnvelope.getPayload()))));
        return responses;
      default:
        log.warn(
            "Ignored message type: {}, from IP: {}",
            clientEventEnvelope.getMessageType(),
            InetExtractor.extract(session.getUserProperties()));
        return responses;
    }
  }

  public Optional<ServerMessageEnvelope> disconnect(final Session session) {
    return chatters.removeSession(session).map(ServerMessageEnvelopeFactory::newPlayerLeft);
  }

  public ServerMessageEnvelope createErrorMessage() {
    return ServerMessageEnvelopeFactory.newErrorMessage();
  }
}
