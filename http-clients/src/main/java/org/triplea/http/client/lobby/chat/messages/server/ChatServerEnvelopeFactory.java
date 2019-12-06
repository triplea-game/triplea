package org.triplea.http.client.lobby.chat.messages.server;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

@UtilityClass
public class ChatServerEnvelopeFactory {

  public ServerMessageEnvelope newEventMessage(final String eventMessage) {
    return ServerMessageEnvelope.packageMessage(
        ChatServerMessageType.CHAT_EVENT.toString(), eventMessage);
  }

  public ServerMessageEnvelope newChatMessage(final ChatMessage chatMessage) {
    return ServerMessageEnvelope.packageMessage(
        ChatServerMessageType.CHAT_MESSAGE.toString(), chatMessage);
  }

  public ServerMessageEnvelope newPlayerJoined(final ChatParticipant chatParticipant) {
    return ServerMessageEnvelope.packageMessage(
        ChatServerMessageType.PLAYER_JOINED.toString(), chatParticipant);
  }

  public ServerMessageEnvelope newPlayerLeft(final PlayerName playerLeft) {
    return ServerMessageEnvelope.packageMessage(
        ChatServerMessageType.PLAYER_LEFT.toString(), playerLeft);
  }

  public ServerMessageEnvelope newSlap(final PlayerSlapped playerSlapped) {
    return ServerMessageEnvelope.packageMessage(
        ChatServerMessageType.PLAYER_SLAPPED.toString(), playerSlapped);
  }

  public ServerMessageEnvelope newStatusUpdate(final StatusUpdate statusUpdate) {
    return ServerMessageEnvelope.packageMessage(
        ChatServerMessageType.STATUS_CHANGED.toString(), statusUpdate);
  }

  public ServerMessageEnvelope newPlayerListing(final List<ChatParticipant> chatters) {
    return ServerMessageEnvelope.packageMessage(
        ChatServerMessageType.PLAYER_LISTING.toString(), new ChatterList(chatters));
  }

  public ServerMessageEnvelope newErrorMessage() {
    return ServerMessageEnvelope.packageMessage(
        ChatServerMessageType.SERVER_ERROR.toString(),
        "Message processing failed, error on server");
  }
}
