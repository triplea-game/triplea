package org.triplea.modules.game.lobby.watcher;

import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingRequest;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingResponse;
import org.triplea.modules.game.listing.GameListing;

/**
 * Verifies that we can do a 'reverse' connection back to the requesting game host (this verifies
 * that their network is well configured to accept connections), if so then we will add their game
 * posting request to the list of available games.
 */
@Builder
class GamePostingModule {
  @Nonnull private final GameListing gameListing;
  @Nonnull private final ConnectivityCheck connectivityCheck;

  public static GamePostingModule build(final GameListing gameListing) {

    return GamePostingModule.builder()
        .gameListing(gameListing)
        .connectivityCheck(new ConnectivityCheck())
        .build();
  }

  GamePostingResponse postGame(final ApiKey apiKey, final GamePostingRequest gamePostingRequest) {
    final boolean canReverseConnect =
        connectivityCheck.canDoReverseConnect(
            gamePostingRequest.getLobbyGame().getHostAddress(),
            gamePostingRequest.getLobbyGame().getHostPort());

    return canReverseConnect
        ? GamePostingResponse.builder()
            .connectivityCheckSucceeded(true)
            .gameId(gameListing.postGame(apiKey, gamePostingRequest))
            .build()
        : GamePostingResponse.builder().connectivityCheckSucceeded(false).build();
  }
}
