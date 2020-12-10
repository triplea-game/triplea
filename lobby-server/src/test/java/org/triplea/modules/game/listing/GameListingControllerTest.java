package org.triplea.modules.game.listing;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.game.lobby.watcher.GameListingClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.LobbyServerTest;
import org.triplea.modules.http.ProtectedEndpointTest;

@DataSet(value = LobbyServerTest.LOBBY_USER_DATASET, useSequenceFiltering = false)
class GameListingControllerTest extends ProtectedEndpointTest<GameListingClient> {

  GameListingControllerTest(final URI host) {
    super(host, AllowedUserRole.HOST, GameListingClient::newClient);
  }

  @Test
  void fetchGames() {
    verifyEndpoint(AllowedUserRole.ANONYMOUS, GameListingClient::fetchGameListing);
  }

  @Test
  void bootGame() {
    verifyEndpoint(AllowedUserRole.MODERATOR, client -> client.bootGame("game-id"));
  }
}
