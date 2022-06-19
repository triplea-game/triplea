package org.triplea.http.client.lobby.game.lobby.watcher;

import feign.RequestLine;

interface LobbyWatcherFeignClient {
  @RequestLine("POST " + LobbyWatcherClient.POST_GAME_PATH)
  GamePostingResponse postGame(GamePostingRequest gamePostingRequest);

  @RequestLine("POST " + LobbyWatcherClient.UPDATE_GAME_PATH)
  void updateGame(UpdateGameRequest updateGameRequest);

  @RequestLine("POST " + LobbyWatcherClient.REMOVE_GAME_PATH)
  void removeGame(String gameId);

  @RequestLine("POST " + LobbyWatcherClient.KEEP_ALIVE_PATH)
  boolean sendKeepAlive(String gameId);

  @RequestLine("POST " + LobbyWatcherClient.UPLOAD_CHAT_PATH)
  String uploadChat(ChatMessageUpload chatMessageUpload);

  @RequestLine("POST " + LobbyWatcherClient.PLAYER_JOINED_PATH)
  String playerJoined(PlayerJoinedNotification playerJoinedNotification);

  @RequestLine("POST " + LobbyWatcherClient.PLAYER_LEFT_PATH)
  String playerLeft(PlayerLeftNotification playerLeftNotification);
}
