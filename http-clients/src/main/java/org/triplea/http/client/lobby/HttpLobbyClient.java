package org.triplea.http.client.lobby;

import java.net.URI;
import lombok.Getter;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.moderator.toolbox.HttpModeratorToolboxClient;
import org.triplea.http.client.lobby.user.account.UserAccountClient;

/** Holder class for the various http clients that access lobby resources. */
@Getter
public class HttpLobbyClient {
  private final HttpModeratorToolboxClient httpModeratorToolboxClient;
  private final GameListingClient gameListingClient;
  private final UserAccountClient userAccountClient;

  private HttpLobbyClient(final URI lobbyUri, final String apiKey) {
    httpModeratorToolboxClient = HttpModeratorToolboxClient.newClient(lobbyUri, apiKey);
    gameListingClient = GameListingClient.newClient(lobbyUri, apiKey);
    userAccountClient = UserAccountClient.newClient(lobbyUri, apiKey);
  }

  public static HttpLobbyClient newClient(final URI lobbyUri, final String apiKey) {
    return new HttpLobbyClient(lobbyUri, apiKey);
  }
}
