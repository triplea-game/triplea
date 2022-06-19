package org.triplea.http.client.lobby.game.lobby.watcher;

import java.net.URI;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.LobbyGame;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

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
  public static final String PLAYER_JOINED_PATH = "/lobby/games/player-joined";
  public static final String PLAYER_LEFT_PATH = "/lobby/games/player-left";
  public static final String UPLOAD_CHAT_PATH = "/lobby/chat/upload";

  private final LobbyWatcherFeignClient lobbyWatcherFeignClient;

  public static LobbyWatcherClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new LobbyWatcherClient(
        HttpClient.newClient(
            LobbyWatcherFeignClient.class,
            serverUri,
            new AuthenticationHeaders(apiKey).createHeaders()));
  }

  public GamePostingResponse postGame(final GamePostingRequest gamePostingRequest) {
    return lobbyWatcherFeignClient.postGame(gamePostingRequest);
  }

  public void updateGame(final String gameId, final LobbyGame lobbyGame) {
    lobbyWatcherFeignClient.updateGame(
        UpdateGameRequest.builder().gameId(gameId).gameData(lobbyGame).build());
  }

  public boolean sendKeepAlive(final String gameId) {
    return lobbyWatcherFeignClient.sendKeepAlive(gameId);
  }

  public void removeGame(final String gameId) {
    lobbyWatcherFeignClient.removeGame(gameId);
  }

  public void uploadChatMessage(final ChatUploadParams uploadChatMessageParams) {
    lobbyWatcherFeignClient.uploadChat(uploadChatMessageParams.toChatMessageUpload());
  }

  public void playerJoined(final String gameId, final UserName playerName) {
    lobbyWatcherFeignClient.playerJoined(
        PlayerJoinedNotification.builder()
            .gameId(gameId)
            .playerName(playerName.getValue())
            .build());
  }

  public void playerLeft(final String gameId, final UserName playerName) {
    lobbyWatcherFeignClient.playerLeft(
        PlayerLeftNotification.builder() //
            .gameId(gameId)
            .playerName(playerName.getValue())
            .build());
  }
}
