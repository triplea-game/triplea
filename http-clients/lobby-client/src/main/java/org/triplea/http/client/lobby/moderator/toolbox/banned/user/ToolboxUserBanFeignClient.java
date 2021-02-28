package org.triplea.http.client.lobby.moderator.toolbox.banned.user;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface ToolboxUserBanFeignClient {

  @RequestLine("GET " + ToolboxUserBanClient.GET_USER_BANS_PATH)
  List<UserBanData> getUserBans(@HeaderMap Map<String, Object> headers);

  @RequestLine("POST " + ToolboxUserBanClient.REMOVE_USER_BAN_PATH)
  void removeUserBan(@HeaderMap Map<String, Object> headers, String banId);

  @RequestLine("POST " + ToolboxUserBanClient.BAN_USER_PATH)
  void banUser(@HeaderMap Map<String, Object> headers, UserBanParams banUserParams);
}
