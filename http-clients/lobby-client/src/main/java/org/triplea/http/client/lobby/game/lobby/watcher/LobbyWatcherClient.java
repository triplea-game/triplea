package org.triplea.http.client.lobby.game.lobby.watcher;

import feign.RequestLine;
import java.net.URI;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.LobbyGame;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Http client for interacting with lobby game listing. Can be used to post, remove, boot, fetch and
 * update games.
 */
public interface LobbyWatcherClient {

  String KEEP_ALIVE_PATH = "/lobby/games/keep-alive";
  String POST_GAME_PATH = "/lobby/games/post-game";
  String UPDATE_GAME_PATH = "/lobby/games/update-game";
  String REMOVE_GAME_PATH = "/lobby/games/remove-game";
  String PLAYER_JOINED_PATH = "/lobby/games/player-joined";
  String PLAYER_LEFT_PATH = "/lobby/games/player-left";

  static LobbyWatcherClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        LobbyWatcherClient.class, serverUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + LobbyWatcherClient.POST_GAME_PATH)
  GamePostingResponse postGame(GamePostingRequest gamePostingRequest);

  @RequestLine("POST " + LobbyWatcherClient.UPDATE_GAME_PATH)
  void updateGame(UpdateGameRequest updateGameRequest);

  default void updateGame(final String gameId, final LobbyGame lobbyGame) {
    updateGame(UpdateGameRequest.builder().gameId(gameId).gameData(lobbyGame).build());
  }

  @RequestLine("POST " + LobbyWatcherClient.KEEP_ALIVE_PATH)
  boolean sendKeepAlive(String gameId);

  @RequestLine("POST " + LobbyWatcherClient.REMOVE_GAME_PATH)
  void removeGame(String gameId);

  @RequestLine("POST " + LobbyWatcherClient.PLAYER_JOINED_PATH)
  String playerJoined(PlayerJoinedNotification playerJoinedNotification);

  default void playerJoined(final String gameId, final UserName playerName) {
    playerJoined(
        PlayerJoinedNotification.builder()
            .gameId(gameId)
            .playerName(playerName.getValue())
            .build());
  }

  @RequestLine("POST " + LobbyWatcherClient.PLAYER_LEFT_PATH)
  String playerLeft(PlayerLeftNotification playerLeftNotification);

  default void playerLeft(final String gameId, final UserName playerName) {
    playerLeft(
        PlayerLeftNotification.builder() //
            .gameId(gameId)
            .playerName(playerName.getValue())
            .build());
  }
}
