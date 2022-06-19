package org.triplea.http.client.lobby.game.lobby.watcher;

import feign.Headers;
import feign.RequestLine;
import java.util.List;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface GameListingFeignClient {
  @RequestLine("GET " + GameListingClient.FETCH_GAMES_PATH)
  List<LobbyGameListing> fetchGameListing();

  @RequestLine("POST " + GameListingClient.BOOT_GAME_PATH)
  void bootGame(String gameId);
}
