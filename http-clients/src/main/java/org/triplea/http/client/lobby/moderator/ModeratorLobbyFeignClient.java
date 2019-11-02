package org.triplea.http.client.lobby.moderator;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
public interface ModeratorLobbyFeignClient {

  @RequestLine("POST " + ModeratorLobbyClient.BAN_PLAYER_PATH)
  void banPlayer(@HeaderMap Map<String, Object> headers, BanPlayerRequest banPlayerRequest);

  @RequestLine("POST " + ModeratorLobbyClient.DISCONNECT_PLAYER_PATH)
  void disconnectPlayer(@HeaderMap Map<String, Object> headers, String value);

  @RequestLine("POST " + ModeratorLobbyClient.DISCONNECT_GAME_PATH)
  void disconnectGame(@HeaderMap Map<String, Object> headers, String gameId);
}
