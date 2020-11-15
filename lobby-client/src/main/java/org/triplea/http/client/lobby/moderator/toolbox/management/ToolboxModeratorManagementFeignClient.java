package org.triplea.http.client.lobby.moderator.toolbox.management;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface ToolboxModeratorManagementFeignClient {
  @RequestLine("GET " + ToolboxModeratorManagementClient.FETCH_MODERATORS_PATH)
  List<ModeratorInfo> fetchModerators(@HeaderMap Map<String, Object> headerMap);

  @RequestLine("GET " + ToolboxModeratorManagementClient.IS_ADMIN_PATH)
  boolean isAdmin(@HeaderMap Map<String, Object> headerMap);

  @RequestLine("POST " + ToolboxModeratorManagementClient.ADD_ADMIN_PATH)
  void addAdmin(@HeaderMap Map<String, Object> headers, String moderatorName);

  @RequestLine("POST " + ToolboxModeratorManagementClient.REMOVE_MOD_PATH)
  void removeMod(@HeaderMap Map<String, Object> headers, String moderatorName);

  @RequestLine("POST " + ToolboxModeratorManagementClient.CHECK_USER_EXISTS_PATH)
  boolean checkUserExists(@HeaderMap Map<String, Object> headers, String username);

  @RequestLine("POST " + ToolboxModeratorManagementClient.ADD_MODERATOR_PATH)
  void addModerator(@HeaderMap Map<String, Object> headers, String username);
}
