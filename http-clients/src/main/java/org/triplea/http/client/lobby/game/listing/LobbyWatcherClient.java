package org.triplea.http.client.lobby.game.listing;

import java.net.URI;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/**
 * Http client for interacting with lobby game listing. Can be used to post, remove, boot, fetch and
 * update games.
 */
@AllArgsConstructor
public class LobbyWatcherClient {
  public static final int KEEP_ALIVE_SECONDS = 20;

  public static final String KEEP_ALIVE_PATH = "/lobby/games/keep-alive";
  public static final String POST_GAME_PATH = "/lobby/games/post-game";
  public static final String UPDATE_GAME_PATH = "/lobby/games/update-game";
  public static final String REMOVE_GAME_PATH = "/lobby/games/remove-game";

  private final AuthenticationHeaders authenticationHeaders;
  private final LobbyWatcherFeignClient gameListingFeignClient;

  public static LobbyWatcherClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new LobbyWatcherClient(
        new AuthenticationHeaders(apiKey),
        new HttpClient<>(LobbyWatcherFeignClient.class, serverUri).get());
  }

  public String postGame(final LobbyGame lobbyGame) {
    return gameListingFeignClient.postGame(authenticationHeaders.createHeaders(), lobbyGame);
  }

  public void updateGame(final String gameId, final LobbyGame lobbyGame) {
    gameListingFeignClient.updateGame(
        authenticationHeaders.createHeaders(),
        UpdateGameRequest.builder().gameId(gameId).gameData(lobbyGame).build());
  }

  public boolean sendKeepAlive(final String gameId) {
    return gameListingFeignClient.sendKeepAlive(authenticationHeaders.createHeaders(), gameId);
  }

  public void removeGame(final String gameId) {
    gameListingFeignClient.removeGame(authenticationHeaders.createHeaders(), gameId);
  }
}
