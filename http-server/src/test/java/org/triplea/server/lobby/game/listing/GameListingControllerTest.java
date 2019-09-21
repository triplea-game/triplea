package org.triplea.server.lobby.game.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.server.http.ProtectedEndpointTest;

class GameListingControllerTest extends ProtectedEndpointTest<GameListingClient> {

  private static final LobbyGame LOBBY_GAME =
      LobbyGame.builder()
          .hostIpAddress("127.0.0.1")
          .hostPort(12)
          .hostName("name")
          .mapName("map")
          .playerCount(3)
          .gameRound(1)
          .epochMilliTimeStarted(Instant.now().toEpochMilli())
          .mapVersion("1")
          .passworded(false)
          .status("WAITING_FOR_PLAYERS")
          .comments("comments")
          .build();

  GameListingControllerTest() {
    super(GameListingClient::newClient);
  }

  @Test
  void postGame() {
    verifyEndpointReturningObject(client -> client.postGame(LOBBY_GAME));
  }

  @Test
  void removeGame() {
    final String gameId = verifyEndpointReturningObject(client -> client.postGame(LOBBY_GAME));
    verifyEndpointReturningVoid(client -> client.removeGame(gameId));
  }

  @Test
  void keepAlive() {
    final String gameId = verifyEndpointReturningObject(client -> client.postGame(LOBBY_GAME));
    final boolean result = verifyEndpointReturningObject(client -> client.sendKeepAlive(gameId));
    assertThat(result, is(true));
  }

  @Test
  void fetchGames() {
    verifyEndpointReturningObject(client -> client.postGame(LOBBY_GAME));
    verifyEndpointReturningCollection(GameListingClient::fetchGameListing);
  }

  @Test
  void updateGame() {
    final String gameId = verifyEndpointReturningObject(client -> client.postGame(LOBBY_GAME));
    verifyEndpointReturningVoid(client -> client.updateGame(gameId, LOBBY_GAME));
  }

  @Test
  void bootGame() {
    final String gameId = verifyEndpointReturningObject(client -> client.postGame(LOBBY_GAME));
    verifyEndpointReturningVoid(client -> client.bootGame(gameId));
  }
}
