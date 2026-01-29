package org.triplea.http.client.lobby.moderator.toolbox.management;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Http client for moderator management. Other than the fetch to display the list of moderators,
 * most actions will be available to a "super-mod" only (super-mod = admin of mods).
 */
public interface ToolboxModeratorManagementClient {
  String FETCH_MODERATORS_PATH = "/lobby/moderator-toolbox/fetch-moderators";
  String IS_ADMIN_PATH = "/lobby/moderator-toolbox/is-admin";
  String CHECK_USER_EXISTS_PATH = "/lobby/moderator-toolbox/does-user-exist";
  String REMOVE_MOD_PATH = "/lobby/moderator-toolbox/admin/remove-mod";
  String ADD_ADMIN_PATH = "/lobby/moderator-toolbox/admin/add-super-mod";
  String ADD_MODERATOR_PATH = "/lobby/moderator-toolbox/admin/add-moderator";

  static ToolboxModeratorManagementClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ToolboxModeratorManagementClient.class,
        serverUri,
        new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("GET " + ToolboxModeratorManagementClient.FETCH_MODERATORS_PATH)
  List<ModeratorInfo> fetchModeratorList();

  @RequestLine("GET " + ToolboxModeratorManagementClient.IS_ADMIN_PATH)
  boolean isCurrentUserAdmin();

  @RequestLine("POST " + ToolboxModeratorManagementClient.ADD_ADMIN_PATH)
  void addAdmin(String moderatorName);

  @RequestLine("POST " + ToolboxModeratorManagementClient.REMOVE_MOD_PATH)
  void removeMod(String moderatorName);

  @RequestLine("POST " + ToolboxModeratorManagementClient.CHECK_USER_EXISTS_PATH)
  boolean checkUserExists(String username);

  @RequestLine("POST " + ToolboxModeratorManagementClient.ADD_MODERATOR_PATH)
  void addModerator(String username);
}
