package org.triplea.http.client.lobby.game.hosting;

import feign.Headers;
import feign.RequestLine;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
public interface GameHostingFeignClient {

  @RequestLine("POST " + GameHostingClient.GAME_HOSTING_REQUEST_PATH)
  GameHostingResponse sendGameHostingRequest();
}
