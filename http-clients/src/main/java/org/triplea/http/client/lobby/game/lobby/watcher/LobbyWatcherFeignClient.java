package org.triplea.http.client.lobby.game.lobby.watcher;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface LobbyWatcherFeignClient {
  @RequestLine("POST " + LobbyWatcherClient.POST_GAME_PATH)
  String postGame(@HeaderMap Map<String, Object> headers, GamePostingRequest gamePostingRequest);

  @RequestLine("POST " + LobbyWatcherClient.UPDATE_GAME_PATH)
  void updateGame(@HeaderMap Map<String, Object> headers, UpdateGameRequest updateGameRequest);

  @RequestLine("POST " + LobbyWatcherClient.REMOVE_GAME_PATH)
  void removeGame(@HeaderMap Map<String, Object> headers, String gameId);

  @RequestLine("POST " + LobbyWatcherClient.KEEP_ALIVE_PATH)
  boolean sendKeepAlive(@HeaderMap Map<String, Object> headers, String gameId);

  @RequestLine("POST " + LobbyWatcherClient.UPLOAD_CHAT_PATH)
  String uploadChat(@HeaderMap Map<String, Object> headers, ChatMessageUpload chatMessageUpload);

  @RequestLine("POST " + LobbyWatcherClient.PLAYER_JOINED_PATH)
  String playerJoined(
      @HeaderMap Map<String, Object> headers, PlayerJoinedNotification playerJoinedNotification);

  @RequestLine("POST " + LobbyWatcherClient.PLAYER_LEFT_PATH)
  String playerLeft(
      @HeaderMap Map<String, Object> headers, PlayerLeftNotification playerLeftNotification);
}
