package org.triplea.http.client.lobby.game.hosting.request;

import feign.RequestLine;
import java.net.URI;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Use this client to request a connection to lobby and to post a game. If the request is
 * successful, the lobby will respond with an API key which can then be used to create a {@code
 * GameListingClient}.
 */
public interface GameHostingClient {
  String GAME_HOSTING_REQUEST_PATH = "/lobby/game-hosting-request";

  static GameHostingClient newClient(final URI lobby) {
    return HttpClient.newClient(
        GameHostingClient.class, lobby, AuthenticationHeaders.systemIdHeaders());
  }

  @RequestLine("POST " + GameHostingClient.GAME_HOSTING_REQUEST_PATH)
  GameHostingResponse sendGameHostingRequest();
}
