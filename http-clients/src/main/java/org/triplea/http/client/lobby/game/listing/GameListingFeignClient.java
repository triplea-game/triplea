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

  @RequestLine("POST " + GameListingClient.BOOT_GAME_PATH)
  void bootGame(@HeaderMap Map<String, Object> headers, String gameId);
}
