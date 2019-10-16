package org.triplea.server.lobby.chat.event.processing;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;
import org.triplea.http.client.lobby.chat.events.server.PlayerJoined;
import org.triplea.http.client.lobby.chat.events.server.PlayerLeft;
import org.triplea.http.client.lobby.chat.events.server.PlayerListing;
import org.triplea.http.client.lobby.chat.events.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope.ServerMessageType;
import org.triplea.http.client.lobby.chat.events.server.StatusUpdate;

@UtilityClass
class ServerEventEnvelopeFactory {

  ServerEventEnvelope newChatMessage(final ChatMessage chatMessage) {
    return ServerEventEnvelope.packageMessage(ServerMessageType.CHAT_MESSAGE, chatMessage);
  }

  ServerEventEnvelope newPlayerJoined(final ChatParticipant chatParticipant) {
    return ServerEventEnvelope.packageMessage(
        ServerMessageType.PLAYER_JOINED, new PlayerJoined(chatParticipant));
  }

  ServerEventEnvelope newPlayerLeft(final PlayerName playerLeft) {
    return ServerEventEnvelope.packageMessage(
        ServerMessageType.PLAYER_LEFT, new PlayerLeft(playerLeft));
  }

  ServerEventEnvelope newSlap(final PlayerSlapped playerSlapped) {
    return ServerEventEnvelope.packageMessage(ServerMessageType.PLAYER_SLAPPED, playerSlapped);
  }

  ServerEventEnvelope newStatusUpdate(final StatusUpdate statusUpdate) {
    return ServerEventEnvelope.packageMessage(ServerMessageType.STATUS_CHANGED, statusUpdate);
  }

  ServerEventEnvelope newPlayerListing(final List<ChatParticipant> chatters) {
    return ServerEventEnvelope.packageMessage(
        ServerMessageType.PLAYER_LISTING, new PlayerListing(chatters));
  }

  static ServerEventEnvelope newErrorMessage() {
    return ServerEventEnvelope.packageMessage(
        ServerMessageType.SERVER_ERROR, "Message processing failed, error on server");
  }
}
