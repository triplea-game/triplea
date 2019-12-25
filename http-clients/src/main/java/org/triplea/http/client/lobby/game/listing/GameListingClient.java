package org.triplea.http.client.lobby.game.listing;

import java.net.URI;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

@Builder
public class GameListingClient {
  public static final String FETCH_GAMES_PATH = "/lobby/games/fetch-games";
  public static final String BOOT_GAME_PATH = "/lobby/games/boot-game";

  @Nonnull private final AuthenticationHeaders authenticationHeaders;
  @Nonnull private final GameListingFeignClient gameListingFeignClient;

  public static GameListingClient newClient(final URI serverUri, final ApiKey apiKey) {
    return GameListingClient.builder()
        .authenticationHeaders(new AuthenticationHeaders(apiKey))
        .gameListingFeignClient(new HttpClient<>(GameListingFeignClient.class, serverUri).get())
        .build();
  }

  public List<LobbyGameListing> fetchGameListing() {
    return gameListingFeignClient.fetchGameListing(authenticationHeaders.createHeaders());
  }

  public void bootGame(final String gameId) {
    gameListingFeignClient.bootGame(authenticationHeaders.createHeaders(), gameId);
  }
}
