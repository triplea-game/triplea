package org.triplea.http.client.lobby.game.listing;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.game.listing.messages.GameListingListeners;
import org.triplea.http.client.lobby.game.listing.messages.GameListingMessageType;
import org.triplea.http.client.web.socket.WebsocketListenerBinding;
import org.triplea.http.client.web.socket.WebsocketListenerFactory;

/**
 * Client for interaction with the lobby game listing. Some operations are synchronous, game listing
 * updates are asynchronous and are pushed over websocket from the server to client listeners.
 */
@Builder
public class GameListingClient {
  public static final String GAME_LISTING_WEBSOCKET_PATH = "/lobby/games/listing-ws";
  public static final String FETCH_GAMES_PATH = "/lobby/games/fetch-games";
  public static final String BOOT_GAME_PATH = "/lobby/games/boot-game";

  @Nonnull private final AuthenticationHeaders authenticationHeaders;
  @Nonnull private final GameListingFeignClient gameListingFeignClient;

  @Nonnull
  private final WebsocketListenerBinding<GameListingMessageType, GameListingListeners>
      websocketListener;

  public static GameListingClient newClient(
      final URI serverUri,
      final ApiKey apiKey,
      final Consumer<String> errorHandler,
      final GameListingListeners gameListingListeners) {
    return GameListingClient.builder()
        .authenticationHeaders(new AuthenticationHeaders(apiKey))
        .gameListingFeignClient(new HttpClient<>(GameListingFeignClient.class, serverUri).get())
        .websocketListener(
            WebsocketListenerFactory.newListener(
                serverUri,
                GAME_LISTING_WEBSOCKET_PATH,
                GameListingMessageType::valueOf,
                errorHandler,
                gameListingListeners))
        .build();
  }

  public List<LobbyGameListing> fetchGameListing() {
    return gameListingFeignClient.fetchGameListing(authenticationHeaders.createHeaders());
  }

  public void bootGame(final String gameId) {
    gameListingFeignClient.bootGame(authenticationHeaders.createHeaders(), gameId);
  }

  public void close() {
    websocketListener.close();
  }
}
