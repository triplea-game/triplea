package org.triplea.server.lobby.chat.event.processing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;
import org.triplea.http.client.lobby.chat.events.server.PlayerJoined;
import org.triplea.http.client.lobby.chat.events.server.PlayerLeft;
import org.triplea.http.client.lobby.chat.events.server.PlayerListing;
import org.triplea.http.client.lobby.chat.events.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope;
import org.triplea.http.client.lobby.chat.events.server.StatusUpdate;

class ServerEventEnvelopeFactoryTest {
  private static final String STATUS = "status";
  private static final String MESSAGE = "message";

  private static final PlayerName PLAYER_NAME = PlayerName.of("player");
  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder().playerName(PLAYER_NAME).isModerator(true).build();

  private final StatusUpdate statusUpdate = new StatusUpdate(PLAYER_NAME, STATUS);
  private final PlayerLeft playerLeft = new PlayerLeft(PLAYER_NAME);
  private final PlayerJoined playerJoined = new PlayerJoined(CHAT_PARTICIPANT);
  private final ChatMessage chatMessage = new ChatMessage(PLAYER_NAME, MESSAGE);
  private final PlayerSlapped playerSlapped =
      PlayerSlapped.builder().slapper(PLAYER_NAME).slapped(PlayerName.of("slapped")).build();
  private final PlayerListing playerListing =
      new PlayerListing(Collections.singletonList(CHAT_PARTICIPANT));

  @Test
  void newChatMessage() {
    final ServerEventEnvelope serverEventEnvelope =
        ServerEventEnvelopeFactory.newChatMessage(chatMessage);

    assertThat(
        serverEventEnvelope.getMessageType(),
        is(ServerEventEnvelope.ServerMessageType.CHAT_MESSAGE));
    assertThat(serverEventEnvelope.toChatMessage(), is(chatMessage));
  }

  @Test
  void newPlayerJoined() {
    final ServerEventEnvelope serverEventEnvelope =
        ServerEventEnvelopeFactory.newPlayerJoined(CHAT_PARTICIPANT);

    assertThat(
        serverEventEnvelope.getMessageType(),
        is(ServerEventEnvelope.ServerMessageType.PLAYER_JOINED));
    assertThat(serverEventEnvelope.toPlayerJoined(), is(playerJoined));
  }

  @Test
  void newPlayerLeft() {
    final ServerEventEnvelope serverEventEnvelope =
        ServerEventEnvelopeFactory.newPlayerLeft(PLAYER_NAME);

    assertThat(
        serverEventEnvelope.getMessageType(),
        is(ServerEventEnvelope.ServerMessageType.PLAYER_LEFT));
    assertThat(serverEventEnvelope.toPlayerLeft(), is(playerLeft));
  }

  @Test
  void newSlap() {
    final ServerEventEnvelope serverEventEnvelope =
        ServerEventEnvelopeFactory.newSlap(playerSlapped);

    assertThat(
        serverEventEnvelope.getMessageType(),
        is(ServerEventEnvelope.ServerMessageType.PLAYER_SLAPPED));
    assertThat(serverEventEnvelope.toPlayerSlapped(), is(playerSlapped));
  }

  @Test
  void newStatusUpdate() {
    final ServerEventEnvelope serverEventEnvelope =
        ServerEventEnvelopeFactory.newStatusUpdate(statusUpdate);

    assertThat(
        serverEventEnvelope.getMessageType(),
        is(ServerEventEnvelope.ServerMessageType.STATUS_CHANGED));
    assertThat(serverEventEnvelope.toPlayerStatusChange(), is(statusUpdate));
  }

  @Test
  void newPlayerListing() {
    final ServerEventEnvelope serverEventEnvelope =
        ServerEventEnvelopeFactory.newPlayerListing(Collections.singletonList(CHAT_PARTICIPANT));

    assertThat(
        serverEventEnvelope.getMessageType(),
        is(ServerEventEnvelope.ServerMessageType.PLAYER_LISTING));
    assertThat(serverEventEnvelope.toPlayerListing(), is(playerListing));
  }

  @Test
  void newErrorMessage() {
    final ServerEventEnvelope serverEventEnvelope = ServerEventEnvelopeFactory.newErrorMessage();

    assertThat(
        serverEventEnvelope.getMessageType(),
        is(ServerEventEnvelope.ServerMessageType.SERVER_ERROR));
    assertThat(serverEventEnvelope.toErrorMessage(), notNullValue());
  }
}
