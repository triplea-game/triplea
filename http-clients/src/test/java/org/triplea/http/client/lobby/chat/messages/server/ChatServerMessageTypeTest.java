package org.triplea.http.client.lobby.chat.messages.server;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.CHAT_EVENT;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.CHAT_MESSAGE;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.PLAYER_JOINED;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.PLAYER_LEFT;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.PLAYER_LISTING;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.PLAYER_SLAPPED;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.SERVER_ERROR;
import static org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType.STATUS_CHANGED;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatMessageListeners;
import org.triplea.http.client.lobby.chat.ChatParticipant;

@ExtendWith(MockitoExtension.class)
class ChatServerMessageTypeTest {
  private static final StatusUpdate STATUS_UPDATE =
      new StatusUpdate(UserName.of("player"), "value");
  private static final ChatMessage CHAT_MESSAGE_DATA =
      new ChatMessage(UserName.of("player"), "message");
  private static final PlayerSlapped PLAYER_SLAPPED_DATA =
      PlayerSlapped.builder()
          .slapped(UserName.of("slapped"))
          .slapper(UserName.of("slapper"))
          .build();
  private static final UserName PLAYER_LEFT_DATA = UserName.of("player");
  private static final ChatParticipant PLAYER_JOINED_DATA =
      ChatParticipant.builder()
          .userName(UserName.of("player-name"))
          .playerChatId(PlayerChatId.newId())
          .build();

  @Mock private Consumer<String> chatEventListener;
  @Mock private Consumer<String> errorListener;
  @Mock private Consumer<StatusUpdate> playerStatusListener;
  @Mock private Consumer<ChatMessage> chatMessageListener;
  @Mock private Consumer<PlayerSlapped> playerSlappedListener;
  @Mock private Consumer<UserName> playerLeftListener;
  @Mock private Consumer<ChatParticipant> playerJoinedListener;
  @Mock private Consumer<ChatterList> connectedListener;

  private ChatMessageListeners chatMessageListeners;

  @BeforeEach
  void setup() {
    chatMessageListeners =
        ChatMessageListeners.builder()
            .chatEventListener(chatEventListener)
            .serverErrorListener(errorListener)
            .playerStatusListener(playerStatusListener)
            .chatMessageListener(chatMessageListener)
            .playerSlappedListener(playerSlappedListener)
            .playerLeftListener(playerLeftListener)
            .playerJoinedListener(playerJoinedListener)
            .connectedListener(connectedListener)
            .build();
  }

  @Test
  void chatEvent() {
    CHAT_EVENT.sendPayloadToListener(
        ChatServerEnvelopeFactory.newEventMessage("value"), chatMessageListeners);

    verify(chatEventListener).accept("value");
  }

  @Test
  void serverError() {
    SERVER_ERROR.sendPayloadToListener(
        ChatServerEnvelopeFactory.newErrorMessage(), chatMessageListeners);

    verify(errorListener).accept(anyString());
  }

  @Test
  void playerStatus() {
    STATUS_CHANGED.sendPayloadToListener(
        ChatServerEnvelopeFactory.newStatusUpdate(STATUS_UPDATE), chatMessageListeners);

    verify(playerStatusListener).accept(STATUS_UPDATE);
  }

  @Test
  void chatMessage() {
    CHAT_MESSAGE.sendPayloadToListener(
        ChatServerEnvelopeFactory.newChatMessage(CHAT_MESSAGE_DATA), chatMessageListeners);

    verify(chatMessageListener).accept(CHAT_MESSAGE_DATA);
  }

  @Test
  void playerSlapped() {
    PLAYER_SLAPPED.sendPayloadToListener(
        ChatServerEnvelopeFactory.newSlap(PLAYER_SLAPPED_DATA), chatMessageListeners);

    verify(playerSlappedListener).accept(PLAYER_SLAPPED_DATA);
  }

  @Test
  void playerLeft() {
    PLAYER_LEFT.sendPayloadToListener(
        ChatServerEnvelopeFactory.newPlayerLeft(PLAYER_LEFT_DATA), chatMessageListeners);

    verify(playerLeftListener).accept(PLAYER_LEFT_DATA);
  }

  @Test
  void playerJoined() {
    PLAYER_JOINED.sendPayloadToListener(
        ChatServerEnvelopeFactory.newPlayerJoined(PLAYER_JOINED_DATA), chatMessageListeners);

    verify(playerJoinedListener).accept(PLAYER_JOINED_DATA);
  }

  @Test
  void playerListing() {
    PLAYER_LISTING.sendPayloadToListener(
        ChatServerEnvelopeFactory.newPlayerListing(List.of()), chatMessageListeners);

    verify(connectedListener).accept(Mockito.any(ChatterList.class));
  }
}
