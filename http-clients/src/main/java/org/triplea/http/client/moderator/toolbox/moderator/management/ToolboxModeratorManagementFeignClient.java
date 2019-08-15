package org.triplea.http.client.moderator.toolbox.moderator.management;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;
import org.triplea.http.client.moderator.toolbox.NewApiKey;

interface ToolboxModeratorManagementFeignClient {
  @RequestLine("GET " + ToolboxModeratorManagementClient.FETCH_MODERATORS_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  List<ModeratorInfo> fetchModerators(@HeaderMap Map<String, Object> headerMap);

  @RequestLine("GET " + ToolboxModeratorManagementClient.IS_SUPER_MOD_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  boolean isSuperMod(@HeaderMap Map<String, Object> headerMap);

  @RequestLine("POST " + ToolboxModeratorManagementClient.ADD_SUPER_MOD_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void addSuperMod(@HeaderMap Map<String, Object> headers, String moderatorName);

  @RequestLine("POST " + ToolboxModeratorManagementClient.REMOVE_MOD_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void removeMod(@HeaderMap Map<String, Object> headers, String moderatorName);

  @RequestLine("POST " + ToolboxModeratorManagementClient.CHECK_USER_EXISTS_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  boolean checkUserExists(@HeaderMap Map<String, Object> headers, String username);

  @RequestLine("POST " + ToolboxModeratorManagementClient.SUPER_MOD_GENERATE_SINGLE_USE_KEY_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  NewApiKey generateSingleUseKey(@HeaderMap Map<String, Object> headers, String moderatorName);

  @RequestLine("POST " + ToolboxModeratorManagementClient.ADD_MODERATOR_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void addModerator(@HeaderMap Map<String, Object> headers, String username);
}
