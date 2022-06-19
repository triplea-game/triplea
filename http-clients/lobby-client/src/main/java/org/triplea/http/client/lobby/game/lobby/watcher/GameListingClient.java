package org.triplea.http.client.lobby.game.lobby.watcher;

import java.net.URI;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Client for interaction with the lobby game listing. Some operations are synchronous, game listing
 * updates are asynchronous and are pushed over websocket from the server to client listeners.
 */
@Builder
public class GameListingClient {
  public static final int KEEP_ALIVE_SECONDS = 20;

  public static final String FETCH_GAMES_PATH = "/lobby/games/fetch-games";
  public static final String BOOT_GAME_PATH = "/lobby/games/boot-game";

  @Nonnull private final GameListingFeignClient gameListingFeignClient;

  public static GameListingClient newClient(final URI serverUri, final ApiKey apiKey) {
    return GameListingClient.builder()
        .gameListingFeignClient(
            HttpClient.newClient(
                GameListingFeignClient.class,
                serverUri,
                new AuthenticationHeaders(apiKey).createHeaders()))
        .build();
  }

  public List<LobbyGameListing> fetchGameListing() {
    return gameListingFeignClient.fetchGameListing();
  }

  public void bootGame(final String gameId) {
    gameListingFeignClient.bootGame(gameId);
  }
}
