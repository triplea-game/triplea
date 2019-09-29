package org.triplea.http.client.lobby;

import java.net.URI;
import lombok.Getter;
import org.triplea.http.client.ApiKey;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.moderator.toolbox.HttpModeratorToolboxClient;

/** Holder class for the various http clients that access lobby resources. */
@Getter
public class HttpLobbyClient {
  private final HttpModeratorToolboxClient httpModeratorToolboxClient;
  private final GameListingClient gameListingClient;

  private HttpLobbyClient(final URI lobbyUri, final ApiKey apiKey) {
    httpModeratorToolboxClient = new HttpModeratorToolboxClient(lobbyUri, apiKey);
    gameListingClient = GameListingClient.newClient(lobbyUri, apiKey);
  }

  public static HttpLobbyClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return new HttpLobbyClient(lobbyUri, apiKey);
  }
}
