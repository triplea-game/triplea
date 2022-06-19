package org.triplea.http.client.lobby.moderator.toolbox.management;

import feign.RequestLine;
import java.util.List;

interface ToolboxModeratorManagementFeignClient {
  @RequestLine("GET " + ToolboxModeratorManagementClient.FETCH_MODERATORS_PATH)
  List<ModeratorInfo> fetchModerators();

  @RequestLine("GET " + ToolboxModeratorManagementClient.IS_ADMIN_PATH)
  boolean isAdmin();

  @RequestLine("POST " + ToolboxModeratorManagementClient.ADD_ADMIN_PATH)
  void addAdmin(String moderatorName);

  @RequestLine("POST " + ToolboxModeratorManagementClient.REMOVE_MOD_PATH)
  void removeMod(String moderatorName);

  @RequestLine("POST " + ToolboxModeratorManagementClient.CHECK_USER_EXISTS_PATH)
  boolean checkUserExists(String username);

  @RequestLine("POST " + ToolboxModeratorManagementClient.ADD_MODERATOR_PATH)
  void addModerator(String username);
}
