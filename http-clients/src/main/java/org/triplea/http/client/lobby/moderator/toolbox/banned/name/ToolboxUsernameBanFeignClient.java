package org.triplea.http.client.lobby.moderator.toolbox.banned.name;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface ToolboxUsernameBanFeignClient {

  @RequestLine("POST " + ToolboxUsernameBanClient.REMOVE_BANNED_USER_NAME_PATH)
  void removeUsernameBan(@HeaderMap Map<String, Object> headers, String username);

  @RequestLine("POST " + ToolboxUsernameBanClient.ADD_BANNED_USER_NAME_PATH)
  void addUsernameBan(@HeaderMap Map<String, Object> headers, String username);

  @RequestLine("GET " + ToolboxUsernameBanClient.GET_BANNED_USER_NAMES_PATH)
  List<UsernameBanData> getUsernameBans(@HeaderMap Map<String, Object> headers);
}
