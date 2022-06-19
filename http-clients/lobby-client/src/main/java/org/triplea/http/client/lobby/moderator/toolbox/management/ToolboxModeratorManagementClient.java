package org.triplea.http.client.lobby.moderator.toolbox.management;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Http client for moderator management. Other than the fetch to display the list of moderators,
 * most actions will be available to a "super-mod" only (super-mod = admin of mods).
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxModeratorManagementClient {
  public static final String FETCH_MODERATORS_PATH = "/moderator-toolbox/fetch-moderators";
  public static final String IS_ADMIN_PATH = "/moderator-toolbox/is-admin";
  public static final String CHECK_USER_EXISTS_PATH = "/moderator-toolbox/does-user-exist";
  public static final String REMOVE_MOD_PATH = "/moderator-toolbox/admin/remove-mod";
  public static final String ADD_ADMIN_PATH = "/moderator-toolbox/admin/add-super-mod";
  public static final String ADD_MODERATOR_PATH = "/moderator-toolbox/admin/add-moderator";

  private final ToolboxModeratorManagementFeignClient client;

  public static ToolboxModeratorManagementClient newClient(
      final URI serverUri, final ApiKey apiKey) {
    return new ToolboxModeratorManagementClient(
        HttpClient.newClient(
            ToolboxModeratorManagementFeignClient.class,
            serverUri,
            new AuthenticationHeaders(apiKey).createHeaders()));
  }

  public List<ModeratorInfo> fetchModeratorList() {
    return client.fetchModerators();
  }

  /** Checks with server if the current user is an 'admin' (moderators are not admin) */
  public boolean isCurrentUserAdmin() {
    return client.isAdmin();
  }

  public void removeMod(final String moderatorName) {
    checkArgument(moderatorName != null);
    client.removeMod(moderatorName);
  }

  public void addAdmin(final String moderatorName) {
    checkArgument(moderatorName != null);
    client.addAdmin(moderatorName);
  }

  public boolean checkUserExists(final String usernameRequested) {
    checkArgument(usernameRequested.length() > 3);
    return client.checkUserExists(usernameRequested);
  }

  public void addModerator(final String username) {
    checkArgument(username != null);
    client.addModerator(username);
  }
}
