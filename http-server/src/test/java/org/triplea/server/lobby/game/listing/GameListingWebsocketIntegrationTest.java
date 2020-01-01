package org.triplea.server.lobby.game.listing;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.lobby.game.listing.LobbyWatcherClient;
import org.triplea.http.client.lobby.game.listing.messages.GameListingListeners;
import org.triplea.server.TestData;
import org.triplea.server.http.AllowedUserRole;
import org.triplea.server.http.DropwizardTest;

@ExtendWith(MockitoExtension.class)
class GameListingWebsocketIntegrationTest extends DropwizardTest {

  @Mock private Consumer<LobbyGameListing> gameUpdatedListener;
  @Mock private Consumer<String> gameRemovedListener;

  private LobbyWatcherClient lobbyWatcherClient;
  private GameListingClient gameListingClient;

  @BeforeEach
  void setup() {
    lobbyWatcherClient =
        LobbyWatcherClient.newClient(localhost, AllowedUserRole.HOST.getAllowedKey());
    gameListingClient =
        GameListingClient.newClient(
            localhost, AllowedUserRole.PLAYER.getAllowedKey(), errMsg -> {});

    gameListingClient.addListeners(
        GameListingListeners.builder()
            .gameUpdated(gameUpdatedListener)
            .gameRemoved(gameRemovedListener)
            .build());
  }

  @Test
  @DisplayName("Post a game, verify listener is notified")
  void postGame() {
    final String gameId = lobbyWatcherClient.postGame(TestData.LOBBY_GAME);

    verify(gameUpdatedListener, timeout(2000L))
        .accept(LobbyGameListing.builder().gameId(gameId).lobbyGame(TestData.LOBBY_GAME).build());
  }

  @Test
  @DisplayName("Post and then remove a game, verify remove listener is notified")
  void removeGame() {
    final String gameId = lobbyWatcherClient.postGame(TestData.LOBBY_GAME);
    lobbyWatcherClient.removeGame(gameId);

    verify(gameRemovedListener, timeout(2000L)).accept(gameId);
  }

  @Test
  @DisplayName("Post and then update a game, verify update listener is notified")
  void gameUpdated() {
    final String gameId = lobbyWatcherClient.postGame(TestData.LOBBY_GAME);
    final LobbyGame updatedGame = TestData.LOBBY_GAME.withComments("new comment");
    lobbyWatcherClient.updateGame(gameId, updatedGame);

    verify(gameUpdatedListener, timeout(2000L))
        .accept(LobbyGameListing.builder().gameId(gameId).lobbyGame(updatedGame).build());
  }
}
