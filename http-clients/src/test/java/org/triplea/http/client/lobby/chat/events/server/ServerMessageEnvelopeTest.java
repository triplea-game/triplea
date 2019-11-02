package org.triplea.http.client.lobby.chat.events.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.gson.Gson;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;

class ServerMessageEnvelopeTest {
  private static final Gson gson = new Gson();

  private static final String STATUS = "status";
  private static final String MESSAGE = "message";
  private static final String ERROR_MESSAGE = "error-message";

  private static final PlayerName PLAYER_NAME = PlayerName.of("player");
  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder().playerName(PLAYER_NAME).isModerator(true).build();

  private final StatusUpdate statusUpdate = new StatusUpdate(PLAYER_NAME, STATUS);
  private final PlayerLeft playerLeft = new PlayerLeft(PLAYER_NAME);
  private final PlayerJoined playerJoined = new PlayerJoined(CHAT_PARTICIPANT);
  private final ChatMessage chatMessage = new ChatMessage(PLAYER_NAME, MESSAGE);
  private final PlayerSlapped playerSlapped =
      new PlayerSlapped(PLAYER_NAME, PlayerName.of("slapped"));
  private final PlayerListing playerListing =
      new PlayerListing(Collections.singletonList(CHAT_PARTICIPANT));

  @Test
  void toPlayerStatusChange() {
    final ServerMessageEnvelope serverEventEnvelope =
        ServerMessageEnvelope.packageMessage(
            ServerMessageEnvelope.ServerMessageType.STATUS_CHANGED, statusUpdate);

    final StatusUpdate result = toAndFromJson(serverEventEnvelope).toPlayerStatusChange();

    assertThat(result, is(statusUpdate));
  }

  private static ServerMessageEnvelope toAndFromJson(
      final ServerMessageEnvelope serverEventEnvelope) {
    final String jsonString = gson.toJson(serverEventEnvelope);
    return gson.fromJson(jsonString, ServerMessageEnvelope.class);
  }

  @Test
  void toPlayerLeft() {
    final ServerMessageEnvelope serverEventEnvelope =
        ServerMessageEnvelope.packageMessage(
            ServerMessageEnvelope.ServerMessageType.PLAYER_LEFT, playerLeft);

    final PlayerLeft result = toAndFromJson(serverEventEnvelope).toPlayerLeft();

    assertThat(result, is(playerLeft));
  }

  @Test
  void toPlayerJoined() {
    final ServerMessageEnvelope serverEventEnvelope =
        ServerMessageEnvelope.packageMessage(
            ServerMessageEnvelope.ServerMessageType.PLAYER_JOINED, playerJoined);

    final PlayerJoined result = toAndFromJson(serverEventEnvelope).toPlayerJoined();

    assertThat(result, is(playerJoined));
  }

  @Test
  void toChatMessage() {
    final ServerMessageEnvelope serverEventEnvelope =
        ServerMessageEnvelope.packageMessage(
            ServerMessageEnvelope.ServerMessageType.CHAT_MESSAGE, chatMessage);

    final ChatMessage result = toAndFromJson(serverEventEnvelope).toChatMessage();

    assertThat(result, is(chatMessage));
  }

  @Test
  void toSlapEvent() {
    final ServerMessageEnvelope serverEventEnvelope =
        ServerMessageEnvelope.packageMessage(
            ServerMessageEnvelope.ServerMessageType.PLAYER_SLAPPED, playerSlapped);

    final PlayerSlapped result = toAndFromJson(serverEventEnvelope).toPlayerSlapped();

    assertThat(result, is(playerSlapped));
  }

  @Test
  void toPlayerListing() {
    final ServerMessageEnvelope serverEventEnvelope =
        ServerMessageEnvelope.packageMessage(
            ServerMessageEnvelope.ServerMessageType.PLAYER_LISTING, playerListing);

    final PlayerListing result = toAndFromJson(serverEventEnvelope).toPlayerListing();

    assertThat(result, is(playerListing));
  }

  @Test
  void toErrorMessge() {
    final ServerMessageEnvelope serverEventEnvelope =
        ServerMessageEnvelope.packageMessage(
            ServerMessageEnvelope.ServerMessageType.SERVER_ERROR, ERROR_MESSAGE);

    final String result = toAndFromJson(serverEventEnvelope).toErrorMessage();

    assertThat(result, is(ERROR_MESSAGE));
  }
}
