package org.triplea.http.client.web.socket.client.connections;

import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.LobbyGame;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.LobbyHttpClientConfig;
import org.triplea.http.client.lobby.HttpLobbyClient;
import org.triplea.http.client.lobby.game.hosting.request.GameHostingResponse;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingRequest;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingResponse;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyWatcherClient;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.WebSocket;
import org.triplea.http.client.web.socket.WebsocketPaths;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.IpAddressParser;
import org.triplea.java.concurrency.AsyncRunner;

/**
 * Represents a connection from a hosted game to lobby. A hosted game can perform actions like send
 * a game update so that the lobby updates the game listing. A hosted game will receive messages
 * from lobby for example like a player banned notification.
 */
@Slf4j
public class GameToLobbyConnection {

  private final HttpLobbyClient lobbyClient;
  private final LobbyWatcherClient lobbyWatcherClient;
  private final WebSocket webSocket;
  @Getter private final InetAddress publicVisibleIp;

  public GameToLobbyConnection(
      final URI lobbyUri,
      final GameHostingResponse gameHostingResponse,
      final Consumer<String> errorHandler) {
    publicVisibleIp = gameHostingResponse.getPublicVisibleIp();
    lobbyClient = HttpLobbyClient.newClient(lobbyUri, ApiKey.of(gameHostingResponse.getApiKey()));

    webSocket =
        GenericWebSocketClient.builder()
            .errorHandler(errorHandler)
            .websocketUri(URI.create(lobbyUri.toString() + WebsocketPaths.GAME_CONNECTIONS))
            .headers(
                Map.of(
                    LobbyHttpClientConfig.VERSION_HEADER,
                    LobbyHttpClientConfig.getConfig().getClientVersion(),
                    LobbyHttpClientConfig.SYSTEM_ID_HEADER,
                    LobbyHttpClientConfig.getConfig().getSystemId()))
            .build();
    webSocket.connect();
    lobbyWatcherClient =
        LobbyWatcherClient.newClient(lobbyClient.getLobbyUri(), lobbyClient.getApiKey());
  }

  public <T extends WebSocketMessage> void addMessageListener(
      final MessageType<T> messageType, final Consumer<T> messageHandler) {
    webSocket.addListener(messageType, messageHandler);
  }

  public GamePostingResponse postGame(final GamePostingRequest gamePostingRequest) {
    return lobbyWatcherClient.postGame(gamePostingRequest);
  }

  public boolean sendKeepAlive(final String gameId) {
    return lobbyWatcherClient.sendKeepAlive(gameId);
  }

  public void updateGame(final String gameId, final LobbyGame lobbyGame) {
    lobbyWatcherClient.updateGame(gameId, lobbyGame);
  }

  public void disconnect(final String gameId) {
    AsyncRunner.runAsync(() -> lobbyWatcherClient.removeGame(gameId))
        .exceptionally(e -> log.info("Could not complete lobby game remove call", e));
  }

  public boolean isPlayerBanned(final String ip) {
    return lobbyClient
        .getRemoteActionsClient()
        .checkIfPlayerIsBanned(IpAddressParser.fromString(ip));
  }

  public void close() {
    webSocket.close();
  }

  public void playerJoined(final String gameId, final UserName playerName) {
    AsyncRunner.runAsync(() -> lobbyWatcherClient.playerJoined(gameId, playerName))
        .exceptionally(e -> log.info("Failed to notify lobby a player connected", e));
  }

  public void playerLeft(final String gameId, final UserName playerName) {
    AsyncRunner.runAsync(() -> lobbyWatcherClient.playerLeft(gameId, playerName))
        .exceptionally(e -> log.info("Failed to notify lobby a player left", e));
  }
}
