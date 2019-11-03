package org.triplea.http.client.lobby.chat;

import java.net.URI;
import java.util.Collection;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.java.Log;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.events.client.ClientMessageEnvelope;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;
import org.triplea.http.client.lobby.chat.events.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.events.server.ServerMessageEnvelope;
import org.triplea.http.client.lobby.chat.events.server.StatusUpdate;
import org.triplea.http.client.web.socket.GenericWebSocketClient;

/** Utility component for {@code LobbyChatClient} to wire inbound messages to callback logic. */
@Log
class InboundChat {

  @Getter(AccessLevel.PACKAGE)
  private final GenericWebSocketClient<ServerMessageEnvelope, ClientMessageEnvelope>
      webSocketClient;

  private final InboundEventHandler inboundEventHandler = new InboundEventHandler();

  InboundChat(final URI chatWebsocketUri) {
    webSocketClient =
        new GenericWebSocketClient<>(
            chatWebsocketUri,
            ServerMessageEnvelope.class,
            inboundEventHandler::handleServerMessage);
  }

  void addPlayerStatusListener(final Consumer<StatusUpdate> playerStatusListener) {
    inboundEventHandler.addPlayerStatusListener(playerStatusListener);
  }

  void addPlayerLeftListener(final Consumer<PlayerName> playerLeftListener) {
    inboundEventHandler.addPlayerLeftListener(playerLeftListener);
  }

  void addPlayerJoinedListener(final Consumer<ChatParticipant> playerJoinedListener) {
    inboundEventHandler.addPlayerJoinedListener(playerJoinedListener);
  }

  void addPlayerSlappedListener(final Consumer<PlayerSlapped> playerSlappedListener) {
    inboundEventHandler.addPlayerSlappedListener(playerSlappedListener);
  }

  void addMessageListener(final Consumer<ChatMessage> messageListener) {
    inboundEventHandler.addMessageListener(messageListener);
  }

  void addConnectedListener(final Consumer<Collection<ChatParticipant>> connectedListener) {
    inboundEventHandler.addConnectedListener(connectedListener);
  }

  void addChatEventListener(final Consumer<String> chatEventListener) {
    inboundEventHandler.addChatEventListener(chatEventListener);
  }

  void addConnectionLostListener(final Consumer<String> connectionLostListener) {
    webSocketClient.addConnectionClosedListener(connectionLostListener);
  }
}
