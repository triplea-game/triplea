package org.triplea.http.client.lobby.game.lobby.watcher;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServerPaths;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Client for interaction with the lobby game listing. Some operations are synchronous, game listing
 * updates are asynchronous and are pushed over websocket from the server to client listeners.
 */
public interface GameListingClient {
  int KEEP_ALIVE_SECONDS = 20;

  static GameListingClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        GameListingClient.class, serverUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("GET " + ServerPaths.FETCH_GAMES_PATH)
  List<LobbyGameListing> fetchGameListing();

  @RequestLine("POST " + ServerPaths.BOOT_GAME_PATH)
  void bootGame(String gameId);
}
