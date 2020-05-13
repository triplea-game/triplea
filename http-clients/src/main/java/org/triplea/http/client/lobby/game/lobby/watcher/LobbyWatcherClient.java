package org.triplea.http.client.lobby.game.lobby.watcher;

import java.net.URI;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.LobbyGame;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/**
 * Http client for interacting with lobby game listing. Can be used to post, remove, boot, fetch and
 * update games.
 */
@AllArgsConstructor
public class LobbyWatcherClient {

  public static final String KEEP_ALIVE_PATH = "/lobby/games/keep-alive";
  public static final String POST_GAME_PATH = "/lobby/games/post-game";
  public static final String UPDATE_GAME_PATH = "/lobby/games/update-game";
  public static final String REMOVE_GAME_PATH = "/lobby/games/remove-game";
  public static final String UPLOAD_CHAT_PATH = "/lobby/chat/upload";

  private final AuthenticationHeaders authenticationHeaders;
  private final LobbyWatcherFeignClient lobbyWatcherFeignClient;

  public static LobbyWatcherClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new LobbyWatcherClient(
        new AuthenticationHeaders(apiKey),
        new HttpClient<>(LobbyWatcherFeignClient.class, serverUri).get());
  }

  public String postGame(final LobbyGame lobbyGame) {
    return lobbyWatcherFeignClient.postGame(authenticationHeaders.createHeaders(), lobbyGame);
  }

  public void updateGame(final String gameId, final LobbyGame lobbyGame) {
    lobbyWatcherFeignClient.updateGame(
        authenticationHeaders.createHeaders(),
        UpdateGameRequest.builder().gameId(gameId).gameData(lobbyGame).build());
  }

  public boolean sendKeepAlive(final String gameId) {
    return lobbyWatcherFeignClient.sendKeepAlive(authenticationHeaders.createHeaders(), gameId);
  }

  public void removeGame(final String gameId) {
    lobbyWatcherFeignClient.removeGame(authenticationHeaders.createHeaders(), gameId);
  }

  public void uploadChatMessage(
      final ApiKey apiKey, final ChatUploadParams uploadChatMessageParams) {
    lobbyWatcherFeignClient.uploadChat(
        authenticationHeaders.createHeaders(), uploadChatMessageParams.toChatMessageUpload(apiKey));
  }
}
