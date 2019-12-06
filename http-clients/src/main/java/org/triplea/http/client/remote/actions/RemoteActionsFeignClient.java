package org.triplea.http.client.remote.actions;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface RemoteActionsFeignClient {
  @RequestLine("POST " + RemoteActionsClient.IS_PLAYER_BANNED_PATH)
  boolean checkIfPlayerIsBanned(@HeaderMap Map<String, Object> headers, String bannedIp);

  @RequestLine("POST " + RemoteActionsClient.SEND_SHUTDOWN_PATH)
  void sendShutdown(@HeaderMap Map<String, Object> headers, String ip);
}
