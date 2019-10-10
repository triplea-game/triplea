package org.triplea.http.client.lobby.game.listing;

import java.net.URI;
import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/**
 * Http client for interacting with lobby game listing. Can be used to post, remove, boot, fetch and
 * update games.
 */
@AllArgsConstructor
public class GameListingClient {
  public static final int KEEP_ALIVE_SECONDS = 10;

  public static final String BOOT_GAME_PATH = "/lobby/games/boot-game";
  public static final String FETCH_GAMES_PATH = "/lobby/games/fetch-games";
  public static final String KEEP_ALIVE_PATH = "/lobby/games/keep-alive";
  public static final String POST_GAME_PATH = "/lobby/games/post-game";
  public static final String UPDATE_GAME_PATH = "/lobby/games/update-game";
  public static final String REMOVE_GAME_PATH = "/lobby/games/remove-game";

  private final AuthenticationHeaders authenticationHeaders;
  private final GameListingFeignClient gameListingFeignClient;

  public static GameListingClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new GameListingClient(
        new AuthenticationHeaders(apiKey),
        new HttpClient<>(GameListingFeignClient.class, serverUri).get());
  }

  public List<LobbyGameListing> fetchGameListing() {
    return gameListingFeignClient.fetchGameListing(authenticationHeaders.createHeaders());
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

  public void bootGame(final String gameId) {
    gameListingFeignClient.bootGame(authenticationHeaders.createHeaders(), gameId);
  }
}
