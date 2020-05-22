package org.triplea.modules.game.listing;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.game.lobby.watcher.GameListingClient;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingRequest;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyWatcherClient;
import org.triplea.modules.TestData;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class GameListingControllerTest extends ProtectedEndpointTest<GameListingClient> {

  private static final GamePostingRequest GAME_POSTING_REQUEST =
      GamePostingRequest.builder().playerNames(List.of()).lobbyGame(TestData.LOBBY_GAME).build();

  private final LobbyWatcherClient lobbyWatcherClient;

  GameListingControllerTest() {
    super(AllowedUserRole.HOST, GameListingClient::newClient);
    lobbyWatcherClient =
        LobbyWatcherClient.newClient(localhost, AllowedUserRole.HOST.getAllowedKey());
  }

  @Test
  void fetchGames() {
    lobbyWatcherClient.postGame(GAME_POSTING_REQUEST);
    verifyEndpointReturningCollection(
        AllowedUserRole.ANONYMOUS, GameListingClient::fetchGameListing);
  }

  @Test
  void bootGame() {
    final String gameId = lobbyWatcherClient.postGame(GAME_POSTING_REQUEST);
    verifyEndpoint(AllowedUserRole.MODERATOR, client -> client.bootGame(gameId));
  }
}
