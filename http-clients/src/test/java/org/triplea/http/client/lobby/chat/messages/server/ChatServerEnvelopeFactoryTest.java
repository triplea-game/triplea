package org.triplea.http.client.lobby.chat.messages.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

class ChatServerEnvelopeFactoryTest {
  private static final String STATUS = "status";
  private static final String MESSAGE = "message";

  private static final PlayerName PLAYER_NAME = PlayerName.of("player");
  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder().playerName(PLAYER_NAME).isModerator(true).build();

  private static final StatusUpdate STATUS_UPDATE = new StatusUpdate(PLAYER_NAME, STATUS);
  private static final ChatMessage CHAT_MESSAGE = new ChatMessage(PLAYER_NAME, MESSAGE);
  private static final PlayerSlapped PLAYER_SLAPPED =
      PlayerSlapped.builder().slapper(PLAYER_NAME).slapped(PlayerName.of("slapped")).build();
  private static final ChatterList PLAYER_LISTING = new ChatterList(List.of(CHAT_PARTICIPANT));

  @Test
  void newChatMessage() {
    final ServerMessageEnvelope serverEventEnvelope =
        ChatServerEnvelopeFactory.newChatMessage(CHAT_MESSAGE);

    assertThat(
        serverEventEnvelope.getMessageType(), is(ChatServerMessageType.CHAT_MESSAGE.toString()));
    assertThat(serverEventEnvelope.getPayload(ChatMessage.class), is(CHAT_MESSAGE));
  }

  @Test
  void newPlayerJoined() {
    final ServerMessageEnvelope serverEventEnvelope =
        ChatServerEnvelopeFactory.newPlayerJoined(CHAT_PARTICIPANT);

    assertThat(
        serverEventEnvelope.getMessageType(), is(ChatServerMessageType.PLAYER_JOINED.toString()));
    assertThat(serverEventEnvelope.getPayload(ChatParticipant.class), is(CHAT_PARTICIPANT));
  }

  @Test
  void newPlayerLeft() {
    final ServerMessageEnvelope serverEventEnvelope =
        ChatServerEnvelopeFactory.newPlayerLeft(PLAYER_NAME);

    assertThat(
        serverEventEnvelope.getMessageType(), is(ChatServerMessageType.PLAYER_LEFT.toString()));
    assertThat(serverEventEnvelope.getPayload(PlayerName.class), is(PLAYER_NAME));
  }

  @Test
  void newSlap() {
    final ServerMessageEnvelope serverEventEnvelope =
        ChatServerEnvelopeFactory.newSlap(PLAYER_SLAPPED);

    assertThat(
        serverEventEnvelope.getMessageType(), is(ChatServerMessageType.PLAYER_SLAPPED.toString()));
    assertThat(serverEventEnvelope.getPayload(PlayerSlapped.class), is(PLAYER_SLAPPED));
  }

  @Test
  void newStatusUpdate() {
    final ServerMessageEnvelope serverEventEnvelope =
        ChatServerEnvelopeFactory.newStatusUpdate(STATUS_UPDATE);

    assertThat(
        serverEventEnvelope.getMessageType(), is(ChatServerMessageType.STATUS_CHANGED.toString()));
    assertThat(serverEventEnvelope.getPayload(StatusUpdate.class), is(STATUS_UPDATE));
  }

  @Test
  void newPlayerListing() {
    final ServerMessageEnvelope serverEventEnvelope =
        ChatServerEnvelopeFactory.newPlayerListing(List.of(CHAT_PARTICIPANT));

    assertThat(
        serverEventEnvelope.getMessageType(), is(ChatServerMessageType.PLAYER_LISTING.toString()));
    assertThat(serverEventEnvelope.getPayload(ChatterList.class), is(PLAYER_LISTING));
  }

  @Test
  void newErrorMessage() {
    final ServerMessageEnvelope serverEventEnvelope = ChatServerEnvelopeFactory.newErrorMessage();

    assertThat(
        serverEventEnvelope.getMessageType(), is(ChatServerMessageType.SERVER_ERROR.toString()));
    assertThat(serverEventEnvelope.getPayload(String.class), is(notNullValue()));
  }
}
