package org.triplea.http.client.moderator.toolbox.banned.user;

import java.util.List;
import java.util.Map;

import org.triplea.http.client.HttpConstants;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;

interface ToolboxUserBanFeignClient {

  @RequestLine("GET " + ToolboxUserBanClient.GET_USER_BANS_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  List<UserBanData> getUserBans(@HeaderMap Map<String, Object> headers);

  @RequestLine("POST " + ToolboxUserBanClient.REMOVE_USER_BAN_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void removeUserBan(@HeaderMap Map<String, Object> headers, String banId);

  @RequestLine("POST " + ToolboxUserBanClient.BAN_USER_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void banUser(@HeaderMap Map<String, Object> headers, UserBanParams banUserParams);
}
