package org.triplea.http.client.lobby.game.listing;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface GameListingFeignClient {
  @RequestLine("GET " + GameListingClient.FETCH_GAMES_PATH)
  List<LobbyGameListing> fetchGameListing(@HeaderMap Map<String, Object> headers);

  @RequestLine("POST " + GameListingClient.POST_GAME_PATH)
  String postGame(@HeaderMap Map<String, Object> headers, LobbyGame lobbyGame);

  @RequestLine("POST " + GameListingClient.UPDATE_GAME_PATH)
  void updateGame(@HeaderMap Map<String, Object> headers, UpdateGameRequest updateGameRequest);

  @RequestLine("POST " + GameListingClient.REMOVE_GAME_PATH)
  void removeGame(@HeaderMap Map<String, Object> headers, String gameId);

  @RequestLine("POST " + GameListingClient.KEEP_ALIVE_PATH)
  boolean sendKeepAlive(@HeaderMap Map<String, Object> headers, String gameId);

  @RequestLine("POST " + GameListingClient.BOOT_GAME_PATH)
  void bootGame(@HeaderMap Map<String, Object> headers, String gameId);
}
