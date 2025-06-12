package org.triplea.http.client.web.socket.client.connections;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.LobbyHttpClientConfig;
import org.triplea.http.client.lobby.HttpLobbyClient;
import org.triplea.http.client.lobby.game.lobby.watcher.GameListingClient;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.http.client.lobby.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.WebSocket;
import org.triplea.http.client.web.socket.WebsocketPaths;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatSentMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ConnectToChatMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerSlapSentMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerStatusUpdateSentMessage;

/**
 * Represents a connection from a player to lobby. A player can do actions like get game listings,
 * send chat messages, slap players, etc.. The lobby will send messages to the player for example
 * like chat messages, slap notifications, ban notifications.
 */
public class PlayerToLobbyConnection {
  @Getter private final HttpLobbyClient httpLobbyClient;
  private final GameListingClient gameListingClient;
  private final WebSocket webSocket;

  @Builder
  public PlayerToLobbyConnection(
      @Nonnull final URI lobbyUri,
      @Nonnull final ApiKey apiKey,
      @Nonnull final Consumer<String> errorHandler) {
    httpLobbyClient = HttpLobbyClient.newClient(lobbyUri, apiKey);
    webSocket =
        GenericWebSocketClient.builder()
            .errorHandler(errorHandler)
            .websocketUri(URI.create(lobbyUri + WebsocketPaths.PLAYER_CONNECTIONS))
            .headers(
                Map.of(
                    LobbyHttpClientConfig.VERSION_HEADER,
                    LobbyHttpClientConfig.getConfig().getClientVersion(),
                    LobbyHttpClientConfig.SYSTEM_ID_HEADER,
                    LobbyHttpClientConfig.getConfig().getSystemId()))
            .build();
    webSocket.connect();
    gameListingClient = httpLobbyClient.newGameListingClient();
  }

  public <T extends WebSocketMessage> void addMessageListener(
      final MessageType<T> messageType, final Consumer<T> messageHandler) {
    webSocket.addListener(messageType, messageHandler);
  }

  public List<LobbyGameListing> fetchGameListing() {
    return gameListingClient.fetchGameListing();
  }

  public void bootGame(final String gameId) {
    gameListingClient.bootGame(gameId);
  }

  public void close() {
    webSocket.close();
  }

  public void sendConnectToChatMessage() {
    webSocket.sendMessage(new ConnectToChatMessage(httpLobbyClient.getApiKey()));
  }

  public void sendChatMessage(final String message) {
    webSocket.sendMessage(new ChatSentMessage(message));
  }

  public void slapPlayer(final UserName userName) {
    webSocket.sendMessage(new PlayerSlapSentMessage(userName));
  }

  public void updateStatus(final String status) {
    webSocket.sendMessage(new PlayerStatusUpdateSentMessage(status));
  }

  public void sendShutdownRequest(final String gameId) {
    httpLobbyClient.getRemoteActionsClient().sendShutdownRequest(gameId);
  }

  /**
   * Adds a listener that is invoked when the server closes the connection or we have an unexpected
   * close connection reason.
   */
  public void addConnectionTerminatedListener(final Consumer<String> closedListener) {
    webSocket.addConnectionTerminatedListener(closedListener);
  }

  /** Adds a listener that is invoked when the client disconnects. */
  public void addConnectionClosedListener(final Runnable closedListener) {
    webSocket.addConnectionClosedListener(closedListener);
  }

  public void disconnectPlayer(final PlayerChatId playerChatId) {
    httpLobbyClient.getModeratorLobbyClient().disconnectPlayer(playerChatId.getValue());
  }

  public void banPlayer(final BanPlayerRequest banPlayerRequest) {
    httpLobbyClient.getModeratorLobbyClient().banPlayer(banPlayerRequest);
  }

  public String fetchEmail() {
    return httpLobbyClient.getUserAccountClient().fetchEmail().getUserEmail();
  }

  public void changeEmail(final String newEmail) {
    httpLobbyClient.getUserAccountClient().changeEmail(newEmail);
  }

  public ModeratorToolboxClient getHttpModeratorToolboxClient() {
    return httpLobbyClient.getModeratorToolboxClient();
  }

  public PlayerSummary fetchPlayerInformation(final PlayerChatId playerChatId) {
    return httpLobbyClient
        .getPlayerLobbyActionsClient()
        .fetchPlayerInformation(playerChatId.getValue());
  }

  public void mutePlayer(final PlayerChatId playerChatId, final long minutes) {
    httpLobbyClient.getModeratorLobbyClient().muteUser(playerChatId, minutes);
  }

  public Collection<String> fetchPlayersInGame(final String gameId) {
    return httpLobbyClient.getPlayerLobbyActionsClient().fetchPlayersInGame(gameId);
  }

  public void addConnectionResetListener(final Runnable listener) {
    webSocket.addConnectionResetListener(listener);
  }
}
