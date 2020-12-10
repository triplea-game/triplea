package org.triplea.modules.game;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.github.database.rider.core.api.dataset.DataSet;
import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.LobbyGame;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpConstants;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingRequest;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingResponse;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyWatcherClient;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.http.client.web.socket.messages.envelopes.game.listing.LobbyGameRemovedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.game.listing.LobbyGameUpdatedMessage;
import org.triplea.modules.TestData;
import org.triplea.modules.game.lobby.watcher.LobbyWatcherController;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.LobbyServerTest;

/*
GameListingWebsocketIntegrationTest > Post a game, verify listener is notified FAILED
    Wanted but not invoked:
    gameUpdatedListener.accept(
        LobbyGameListing(gameId=690deee5-8cf7-4815-82b3-d0cc9424fa53,
        lobbyGame=LobbyGame(hostAddress=127.0.0.1, hostPort=12, hostName=name,
         mapName=map, playerCount=3, gameRound=1, epochMilliTimeStarted=1599358874438,
         mapVersion=1, passworded=false, status=Waiting For Players, comments=comments))
    );
    -> at org.triplea.modules.game.GameListingWebsocketIntegrationTest.verifyPostGame(
       GameListingWebsocketIntegrationTest.java:94)
    Actually, there were zero interactions with this mock.
        at org.triplea.modules.game.GameListingWebsocketIntegrationTest.verifyPostGame(
        GameListingWebsocketIntegrationTest.java:94)
 */
@Disabled // Disabled due to flakiness, the above error is frequenlty seen and needs to be resolved.
@ExtendWith(MockitoExtension.class)
@DataSet(value = "integration/game_hosting_api_key.yml,", useSequenceFiltering = false)
@RequiredArgsConstructor
class GameListingWebsocketIntegrationTest extends LobbyServerTest {
  private static final GamePostingRequest GAME_POSTING_REQUEST =
      GamePostingRequest.builder().playerNames(List.of()).lobbyGame(TestData.LOBBY_GAME).build();

  private final URI localhost;

  @Mock private Consumer<LobbyGameListing> gameUpdatedListener;
  @Mock private Consumer<String> gameRemovedListener;

  private LobbyWatcherClient lobbyWatcherClient;

  private GamePostingTestOverrideClient gamePostingTestOverrideClient;

  /**
   * A special test-only HTTP client that can post games to lobby without a reverse connectivity
   * check. This allows us to post games without actually hosting a game.
   */
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  private interface GamePostingTestOverrideClient {
    /** Posts a game, for test-only, returns a game-id from server. */
    @RequestLine("POST " + LobbyWatcherController.TEST_ONLY_GAME_POSTING_PATH)
    GamePostingResponse postGame(
        @HeaderMap Map<String, Object> headers, GamePostingRequest gamePostingRequest);
  }

  @BeforeEach
  void setUp() {
    gamePostingTestOverrideClient =
        new HttpClient<>(GamePostingTestOverrideClient.class, localhost).get();

    lobbyWatcherClient =
        LobbyWatcherClient.newClient(localhost, AllowedUserRole.HOST.getAllowedKey());

    final var playerToLobbyConnection =
        new PlayerToLobbyConnection(
            localhost,
            LobbyServerTest.CHATTER_API_KEY,
            error -> {
              throw new AssertionError(error);
            });
    playerToLobbyConnection.addMessageListener(
        LobbyGameUpdatedMessage.TYPE,
        messageContext -> gameUpdatedListener.accept(messageContext.getLobbyGameListing()));
    playerToLobbyConnection.addMessageListener(
        LobbyGameRemovedMessage.TYPE,
        messageContext -> gameRemovedListener.accept(messageContext.getGameId()));
  }

  @Test
  @DisplayName("Post a game, verify listener is notified")
  void verifyPostGame() {
    final String gameId = postGame();

    verify(gameUpdatedListener, timeout(2000L))
        .accept(
            LobbyGameListing.builder()
                .gameId(gameId)
                .lobbyGame(GAME_POSTING_REQUEST.getLobbyGame())
                .build());
  }

  private String postGame() {
    return gamePostingTestOverrideClient
        .postGame(
            new AuthenticationHeaders(AllowedUserRole.HOST.getAllowedKey()).createHeaders(),
            GAME_POSTING_REQUEST)
        .getGameId();
  }

  @Test
  @DisplayName("Post and then remove a game, verify remove listener is notified")
  void removeGame() {
    final String gameId = postGame();
    lobbyWatcherClient.removeGame(gameId);

    verify(gameRemovedListener, timeout(2000L).atLeastOnce()).accept(gameId);
  }

  @Test
  @DisplayName("Post and then update a game, verify update listener is notified")
  void gameUpdated() {
    final String gameId = postGame();
    final LobbyGame updatedGame = TestData.LOBBY_GAME.withComments("new comment");
    lobbyWatcherClient.updateGame(gameId, updatedGame);

    verify(gameUpdatedListener, timeout(2000L))
        .accept(LobbyGameListing.builder().gameId(gameId).lobbyGame(updatedGame).build());
  }
}
