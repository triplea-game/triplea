package org.triplea.http.client.lobby;

import java.net.URI;
import java.util.function.Consumer;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.chat.LobbyChatClient;
import org.triplea.http.client.lobby.game.ConnectivityCheckClient;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.http.client.lobby.moderator.toolbox.HttpModeratorToolboxClient;
import org.triplea.http.client.lobby.user.account.UserAccountClient;
import org.triplea.http.client.remote.actions.RemoteActionsClient;

/** Holder class for the various http clients that access lobby resources. */
@Getter
public class HttpLobbyClient {
  private final ConnectivityCheckClient connectivityCheckClient;
  private final GameListingClient gameListingClient;
  private final HttpModeratorToolboxClient httpModeratorToolboxClient;
  private final LobbyChatClient lobbyChatClient;
  private final ModeratorChatClient moderatorLobbyClient;
  private final UserAccountClient userAccountClient;
  private final RemoteActionsClient remoteActionsClient;

  private HttpLobbyClient(
      final URI lobbyUri, final ApiKey apiKey, final Consumer<String> errorHandler) {
    connectivityCheckClient = ConnectivityCheckClient.newClient(lobbyUri, apiKey);
    gameListingClient = GameListingClient.newClient(lobbyUri, apiKey, errorHandler);
    httpModeratorToolboxClient = HttpModeratorToolboxClient.newClient(lobbyUri, apiKey);
    lobbyChatClient = LobbyChatClient.newClient(lobbyUri, apiKey, errorHandler);
    moderatorLobbyClient = ModeratorChatClient.newClient(lobbyUri, apiKey);
    userAccountClient = UserAccountClient.newClient(lobbyUri, apiKey);
    remoteActionsClient = new RemoteActionsClient(lobbyUri, apiKey);
  }

  public static HttpLobbyClient newClient(
      final URI lobbyUri, final ApiKey apiKey, final Consumer<String> errorHandler) {
    return new HttpLobbyClient(lobbyUri, apiKey, errorHandler);
  }

  /**
   * Connection closed listener is invoked whenever the underlying connection is closed, whether by
   * ur or remote server.
   */
  public void addConnectionClosedListener(final Runnable connectionClosedListener) {
    lobbyChatClient.addConnectionClosedListener(connectionClosedListener);
  }
}
