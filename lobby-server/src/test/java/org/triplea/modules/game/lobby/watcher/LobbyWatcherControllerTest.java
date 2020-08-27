package org.triplea.modules.game.lobby.watcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.net.URI;
import java.util.List;
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
      GamePostingRequest.builder().playerNames(List.of()).lobbyGame(TestData.LOBBY_GAME).build();

  LobbyWatcherControllerTest(final URI localhost) {
    super(localhost, AllowedUserRole.HOST, LobbyWatcherClient::newClient);
  }

  @Test
  void postGame() {
    verifyEndpoint(client -> client.postGame(GAME_POSTING_REQUEST));
  }

  @Test
  void removeGame() {
    verifyEndpoint(client -> client.removeGame("game-id"));
  }

  @Test
  void keepAlive() {
    final boolean result = verifyEndpointReturningObject(client -> client.sendKeepAlive("game-id"));
    assertThat(result, is(false));
  }

  @Test
  void updateGame() {
    verifyEndpointReturningObject(client -> client.postGame(GAME_POSTING_REQUEST));
  }

  @Test
  void uploadChat() {
    verifyEndpoint(
        client ->
            client.uploadChatMessage(
                AllowedUserRole.HOST.getAllowedKey(),
                ChatUploadParams.builder()
                    .fromPlayer(UserName.of("player"))
                    .chatMessage("chat")
                    .gameId("game-id")
                    .build()));
  }

  @Test
  void notifyPlayerJoined() {
    verifyEndpoint(client -> client.playerJoined("game-id", UserName.of("player-0")));
  }

  @Test
  void notifyPlayerLeft() {
    verifyEndpoint(client -> client.playerJoined("game-id", UserName.of("player-1")));
  }
}
