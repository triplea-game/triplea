package org.triplea.http.client.lobby;

import java.net.URI;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.chat.LobbyChatClient;
import org.triplea.http.client.lobby.game.ConnectivityCheckClient;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.moderator.toolbox.HttpModeratorToolboxClient;
import org.triplea.http.client.lobby.user.account.UserAccountClient;

/** Holder class for the various http clients that access lobby resources. */
@Getter
public class HttpLobbyClient {
  public static final String PROTOCOL = "http://";

  private final ConnectivityCheckClient connectivityCheckClient;
  private final GameListingClient gameListingClient;
  private final HttpModeratorToolboxClient httpModeratorToolboxClient;
  private final UserAccountClient userAccountClient;
  private final LobbyChatClient lobbyChatClient;

  private HttpLobbyClient(final URI lobbyUri, final ApiKey apiKey) {
    connectivityCheckClient = ConnectivityCheckClient.newClient(lobbyUri, apiKey);
    gameListingClient = GameListingClient.newClient(lobbyUri, apiKey);
    httpModeratorToolboxClient = HttpModeratorToolboxClient.newClient(lobbyUri, apiKey);
    userAccountClient = UserAccountClient.newClient(lobbyUri, apiKey);
    lobbyChatClient = LobbyChatClient.newClient(lobbyUri, apiKey);
  }

  public static HttpLobbyClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return new HttpLobbyClient(lobbyUri, apiKey);
  }
}
