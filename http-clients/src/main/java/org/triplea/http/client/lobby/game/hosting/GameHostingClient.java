package org.triplea.http.client.lobby.game.hosting;

import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.SystemIdHeader;

/**
 * Use this client to request a connection to lobby and to post a game. If the request is
 * successful, the lobby will respond with an API key which can then be used to create a {@code
 * GameListingClient}.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameHostingClient {
  public static final String GAME_HOSTING_REQUEST_PATH = "/lobby/game-hosting-request";

  private final GameHostingFeignClient gameHostingFeignClient;

  public static GameHostingClient newClient(final URI lobby) {
    return new GameHostingClient(new HttpClient<>(GameHostingFeignClient.class, lobby).get());
  }

  public GameHostingResponse sendGameHostingRequest() {
    return gameHostingFeignClient.sendGameHostingRequest(SystemIdHeader.headers());
  }
}
