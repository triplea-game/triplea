package org.triplea.http.client.lobby.chat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;
import org.triplea.http.client.lobby.chat.events.server.PlayerLeft;
import org.triplea.http.client.lobby.chat.events.server.PlayerListing;
import org.triplea.http.client.lobby.chat.events.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope.ServerMessageType;
import org.triplea.http.client.lobby.chat.events.server.StatusUpdate;

@ExtendWith(MockitoExtension.class)
class InboundEventHandlerTest {
  private static final List<ChatParticipant> chatters = new ArrayList<>();
  private static final PlayerListing PLAYER_LISTING = new PlayerListing(chatters);
  private static final PlayerName PLAYER_NAME = PlayerName.of("player");
  private static final StatusUpdate STATUS_UPDATE = new StatusUpdate(PLAYER_NAME, "");
  private static final PlayerLeft PLAYER_LEFT = new PlayerLeft(PLAYER_NAME);
  private static final PlayerSlapped PLAYER_SLAPPED =
      PlayerSlapped.builder().slapper(PLAYER_NAME).slapped(PlayerName.of("slapped")).build();
  private static final ChatMessage CHAT_MESSAGE = new ChatMessage(PLAYER_NAME, "message");

  @Mock private Consumer<StatusUpdate> playerStatusListener;
  @Mock private Consumer<PlayerName> playerLeftListener;
  @Mock private Consumer<ChatParticipant> playerJoinedListener;
  @Mock private Consumer<PlayerSlapped> playerSlappedListener;
  @Mock private Consumer<ChatMessage> messageListener;
  @Mock private Consumer<Collection<ChatParticipant>> connectedListener;

  private InboundEventHandler inboundEventHandler;

  @Mock private ServerEventEnvelope serverEventEnvelope;

  @BeforeEach
  void setup() {
    inboundEventHandler = new InboundEventHandler();
    inboundEventHandler.addPlayerStatusListener(playerStatusListener);
    inboundEventHandler.addPlayerLeftListener(playerLeftListener);
    inboundEventHandler.addPlayerJoinedListener(playerJoinedListener);
    inboundEventHandler.addPlayerSlappedListener(playerSlappedListener);
    inboundEventHandler.addMessageListener(messageListener);
    inboundEventHandler.addConnectedListener(connectedListener);
  }

  @Test
  void playerListing() {
    when(serverEventEnvelope.getMessageType()).thenReturn(ServerMessageType.PLAYER_LISTING);
    when(serverEventEnvelope.toPlayerListing()).thenReturn(PLAYER_LISTING);

    inboundEventHandler.handleServerMessage(serverEventEnvelope);

    verify(connectedListener).accept(chatters);
  }

  @Test
  void statusChanged() {
    when(serverEventEnvelope.getMessageType()).thenReturn(ServerMessageType.STATUS_CHANGED);
    when(serverEventEnvelope.toPlayerStatusChange()).thenReturn(STATUS_UPDATE);

    inboundEventHandler.handleServerMessage(serverEventEnvelope);

    verify(playerStatusListener).accept(STATUS_UPDATE);
  }

  @Test
  void playerLeft() {
    when(serverEventEnvelope.getMessageType()).thenReturn(ServerMessageType.PLAYER_LEFT);
    when(serverEventEnvelope.toPlayerLeft()).thenReturn(PLAYER_LEFT);

    inboundEventHandler.handleServerMessage(serverEventEnvelope);

    verify(playerLeftListener).accept(PLAYER_LEFT.getPlayerName());
  }

  @Test
  void playerSlapped() {
    when(serverEventEnvelope.getMessageType()).thenReturn(ServerMessageType.PLAYER_SLAPPED);
    when(serverEventEnvelope.toPlayerSlapped()).thenReturn(PLAYER_SLAPPED);

    inboundEventHandler.handleServerMessage(serverEventEnvelope);

    verify(playerSlappedListener).accept(PLAYER_SLAPPED);
  }

  @Test
  void chatMessage() {
    when(serverEventEnvelope.getMessageType()).thenReturn(ServerMessageType.CHAT_MESSAGE);
    when(serverEventEnvelope.toChatMessage()).thenReturn(CHAT_MESSAGE);

    inboundEventHandler.handleServerMessage(serverEventEnvelope);

    verify(messageListener).accept(CHAT_MESSAGE);
  }

  @Test
  void serverError() {
    when(serverEventEnvelope.getMessageType()).thenReturn(ServerMessageType.SERVER_ERROR);
    when(serverEventEnvelope.toErrorMessage()).thenReturn("error-message");

    inboundEventHandler.handleServerMessage(serverEventEnvelope);

    verify(serverEventEnvelope).toErrorMessage();
  }
}
