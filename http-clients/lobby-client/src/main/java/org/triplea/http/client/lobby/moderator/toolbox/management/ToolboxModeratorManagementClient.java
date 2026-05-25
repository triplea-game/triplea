package org.triplea.http.client.lobby.moderator.toolbox.management;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServerPaths;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Http client for moderator management. Other than the fetch to display the list of moderators,
 * most actions will be available to a "super-mod" only (super-mod = admin of mods).
 */
public interface ToolboxModeratorManagementClient {

  static ToolboxModeratorManagementClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ToolboxModeratorManagementClient.class,
        serverUri,
        new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("GET " + ServerPaths.FETCH_MODERATORS_PATH)
  List<ModeratorInfo> fetchModeratorList();

  @RequestLine("GET " + ServerPaths.IS_ADMIN_PATH)
  boolean isCurrentUserAdmin();

  @RequestLine("POST " + ServerPaths.ADD_ADMIN_PATH)
  void addAdmin(String moderatorName);

  @RequestLine("POST " + ServerPaths.REMOVE_MOD_PATH)
  void removeMod(String moderatorName);

  @RequestLine("POST " + ServerPaths.CHECK_USER_EXISTS_PATH)
  boolean checkUserExists(String username);

  @RequestLine("POST " + ServerPaths.ADD_MODERATOR_PATH)
  void addModerator(String username);
}
