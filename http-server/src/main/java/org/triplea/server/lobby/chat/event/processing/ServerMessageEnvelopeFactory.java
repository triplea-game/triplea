package org.triplea.server.lobby.chat.event.processing;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.events.server.ChatEvent;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;
import org.triplea.http.client.lobby.chat.events.server.PlayerJoined;
import org.triplea.http.client.lobby.chat.events.server.PlayerLeft;
import org.triplea.http.client.lobby.chat.events.server.PlayerListing;
import org.triplea.http.client.lobby.chat.events.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.events.server.ServerMessageEnvelope;
import org.triplea.http.client.lobby.chat.events.server.ServerMessageEnvelope.ServerMessageType;
import org.triplea.http.client.lobby.chat.events.server.StatusUpdate;

@UtilityClass
public class ServerMessageEnvelopeFactory {

  public ServerMessageEnvelope newEventMessage(final String eventMessage) {
    return ServerMessageEnvelope.packageMessage(
        ServerMessageType.CHAT_EVENT, new ChatEvent(eventMessage));
  }

  ServerMessageEnvelope newChatMessage(final ChatMessage chatMessage) {
    return ServerMessageEnvelope.packageMessage(ServerMessageType.CHAT_MESSAGE, chatMessage);
  }

  ServerMessageEnvelope newPlayerJoined(final ChatParticipant chatParticipant) {
    return ServerMessageEnvelope.packageMessage(
        ServerMessageType.PLAYER_JOINED, new PlayerJoined(chatParticipant));
  }

  ServerMessageEnvelope newPlayerLeft(final PlayerName playerLeft) {
    return ServerMessageEnvelope.packageMessage(
        ServerMessageType.PLAYER_LEFT, new PlayerLeft(playerLeft));
  }

  ServerMessageEnvelope newSlap(final PlayerSlapped playerSlapped) {
    return ServerMessageEnvelope.packageMessage(ServerMessageType.PLAYER_SLAPPED, playerSlapped);
  }

  ServerMessageEnvelope newStatusUpdate(final StatusUpdate statusUpdate) {
    return ServerMessageEnvelope.packageMessage(ServerMessageType.STATUS_CHANGED, statusUpdate);
  }

  ServerMessageEnvelope newPlayerListing(final List<ChatParticipant> chatters) {
    return ServerMessageEnvelope.packageMessage(
        ServerMessageType.PLAYER_LISTING, new PlayerListing(chatters));
  }

  ServerMessageEnvelope newErrorMessage() {
    return ServerMessageEnvelope.packageMessage(
        ServerMessageType.SERVER_ERROR, "Message processing failed, error on server");
  }
}
