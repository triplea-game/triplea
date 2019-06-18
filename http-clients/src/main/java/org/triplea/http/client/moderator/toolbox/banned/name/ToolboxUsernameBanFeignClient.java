package org.triplea.http.client.moderator.toolbox.banned.name;

import java.util.List;
import java.util.Map;

import org.triplea.http.client.HttpConstants;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;

interface ToolboxUsernameBanFeignClient {

  @RequestLine("POST " + ToolboxUsernameBanClient.REMOVE_BANNED_USER_NAME_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void removeUsernameBan(@HeaderMap Map<String, Object> headers, String username);

  @RequestLine("POST " + ToolboxUsernameBanClient.ADD_BANNED_USER_NAME_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void addUsernameBan(@HeaderMap Map<String, Object> headers, String username);

  @RequestLine("GET " + ToolboxUsernameBanClient.GET_BANNED_USER_NAMES_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  List<UsernameBanData> getUsernameBans(@HeaderMap Map<String, Object> headers);
}
