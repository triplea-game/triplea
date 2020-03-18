package games.strategy.engine.lobby.connection;

import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;
import lombok.extern.java.Log;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.HttpLobbyClient;
import org.triplea.http.client.lobby.chat.ChatMessageListeners;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.game.listing.messages.GameListingListeners;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.http.client.lobby.moderator.toolbox.HttpModeratorToolboxClient;

/**
 * Represents a connection from a player to lobby. A player can do actions like get game listings,
 * send chat messages, slap players, etc.. The lobby will send messages to the player for example
 * like chat messages, slap notifications, ban notifications.
 */
@Log
public class PlayerToLobbyConnection {
  private final HttpLobbyClient httpLobbyClient;
  private GameListingListeners gameListingListeners;
  private GameListingClient gameListingClient;

  public PlayerToLobbyConnection(final URI lobbyUri, final ApiKey apiKey) {
    httpLobbyClient = HttpLobbyClient.newClient(lobbyUri, apiKey, log::severe);
  }

  public void addGameListingListener(final GameListingListeners gameListingListeners) {
    this.gameListingListeners = gameListingListeners;
    gameListingClient = httpLobbyClient.newGameListingClient(gameListingListeners);

    gameListingClient
        .fetchGameListing()
        .forEach(listing -> gameListingListeners.getGameUpdated().accept(listing));
  }

  public void bootGame(final String gameId) {
    Preconditions.checkNotNull(gameListingListeners);

    GameListingClient.newClient(
            httpLobbyClient.getLobbyUri(),
            httpLobbyClient.getApiKey(),
            log::severe,
            gameListingListeners)
        .bootGame(gameId);
  }

  public void changePassword(final String password) {
    httpLobbyClient.getUserAccountClient().changePassword(password);
  }

  public void addChatMessageListeners(final ChatMessageListeners listeners) {
    httpLobbyClient.getLobbyChatClient().setChatMessageListeners(listeners);
  }

  public Collection<ChatParticipant> connect() {
    return httpLobbyClient.getLobbyChatClient().connect();
  }

  public void close() {
    httpLobbyClient.getLobbyChatClient().close();
  }

  public void sendChatMessage(final String message) {
    httpLobbyClient.getLobbyChatClient().sendChatMessage(message);
  }

  public void slapPlayer(final UserName userName) {
    httpLobbyClient.getLobbyChatClient().slapPlayer(userName);
  }

  public void updateStatus(final String status) {
    httpLobbyClient.getLobbyChatClient().updateStatus(status);
  }

  public void sendShutdownRequest(final InetAddress ipAddress) {
    httpLobbyClient.getRemoteActionsClient().sendShutdownRequest(ipAddress);
  }

  public void addConnectionClosedListener(final Runnable closedListener) {
    httpLobbyClient.getLobbyChatClient().addConnectionClosedListener(closedListener);
  }

  public void disconnectPlayer(final PlayerChatId playerChatId) {
    httpLobbyClient.getModeratorLobbyClient().disconnectPlayer(playerChatId);
  }

  public void banPlayer(final BanPlayerRequest banPlayerRequest) {
    httpLobbyClient.getModeratorLobbyClient().banPlayer(banPlayerRequest);
  }

  public String fetchEmail() {
    return httpLobbyClient.getUserAccountClient().fetchEmail();
  }

  public void changeEmail(final String newEmail) {
    httpLobbyClient.getUserAccountClient().changeEmail(newEmail);
  }

  public HttpModeratorToolboxClient getHttpModeratorToolboxClient() {
    return httpLobbyClient.getHttpModeratorToolboxClient();
  }
}
