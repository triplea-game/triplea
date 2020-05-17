package org.triplea.modules.game.lobby.watcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.game.lobby.watcher.ChatUploadParams;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingRequest;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyWatcherClient;
import org.triplea.modules.TestData;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class LobbyWatcherControllerTest extends ProtectedEndpointTest<LobbyWatcherClient> {
  private static final GamePostingRequest GAME_POSTING_REQUEST =
      GamePostingRequest.builder().lobbyGame(TestData.LOBBY_GAME).build();

  LobbyWatcherControllerTest() {
    super(AllowedUserRole.HOST, LobbyWatcherClient::newClient);
  }

  @Test
  void postGame() {
    verifyEndpoint(client -> client.postGame(GAME_POSTING_REQUEST));
  }

  @Test
  void removeGame() {
    final String gameId =
        verifyEndpointReturningObject(client -> client.postGame(GAME_POSTING_REQUEST));
    verifyEndpoint(client -> client.removeGame(gameId));
  }

  @Test
  void keepAlive() {
    final String gameId =
        verifyEndpointReturningObject(client -> client.postGame(GAME_POSTING_REQUEST));
    final boolean result = verifyEndpointReturningObject(client -> client.sendKeepAlive(gameId));
    assertThat(result, is(true));
  }

  @Test
  void updateGame() {
    final String gameId =
        verifyEndpointReturningObject(client -> client.postGame(GAME_POSTING_REQUEST));
    verifyEndpoint(client -> client.updateGame(gameId, GAME_POSTING_REQUEST.getLobbyGame()));
  }

  @Test
  void uploadChat() {
    final String gameId =
        verifyEndpointReturningObject(client -> client.postGame(GAME_POSTING_REQUEST));

    verifyEndpoint(
        client ->
            client.uploadChatMessage(
                AllowedUserRole.HOST.getAllowedKey(),
                ChatUploadParams.builder()
                    .fromPlayer(UserName.of("player"))
                    .chatMessage("chat")
                    .gameId(gameId)
                    .build()));
  }
}
