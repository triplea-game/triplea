package org.triplea.modules.game.listing;

import org.junit.jupiter.api.Test;
import org.triplea.domain.data.LobbyGame;
import org.triplea.http.client.lobby.game.lobby.watcher.GameListingClient;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyWatcherClient;
import org.triplea.modules.TestData;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class GameListingControllerTest extends ProtectedEndpointTest<GameListingClient> {

  private static final LobbyGame LOBBY_GAME = TestData.LOBBY_GAME;

  private final LobbyWatcherClient lobbyWatcherClient;

  GameListingControllerTest() {
    super(AllowedUserRole.HOST, GameListingClient::newClient);
    lobbyWatcherClient =
        LobbyWatcherClient.newClient(localhost, AllowedUserRole.HOST.getAllowedKey());
  }

  @Test
  void fetchGames() {
    lobbyWatcherClient.postGame(LOBBY_GAME);
    verifyEndpointReturningCollection(
        AllowedUserRole.ANONYMOUS, GameListingClient::fetchGameListing);
  }

  @Test
  void bootGame() {
    final String gameId = lobbyWatcherClient.postGame(LOBBY_GAME);
    verifyEndpoint(AllowedUserRole.MODERATOR, client -> client.bootGame(gameId));
  }
}
