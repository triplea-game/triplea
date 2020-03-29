package org.triplea.http.client.lobby.game;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface ConnectivityCheckFeignClient {
  @RequestLine("POST " + ConnectivityCheckClient.CONNECTIVITY_CHECK_PATH)
  boolean checkConnectivity(@HeaderMap Map<String, Object> headers, String gameId);
}
