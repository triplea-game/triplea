package org.triplea.http.client.moderator.toolbox.moderator.management;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;

/**
 * Http client for moderator management. Other than the fetch to display the list of moderators,
 * most actions will be available to a "super-mod" only (super-mod = admin of mods).
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxModeratorManagementClient {
  public static final String FETCH_MODERATORS_PATH = "/moderator-toolbox/fetch-moderators";
  public static final String IS_SUPER_MOD_PATH = "/moderator-toolbox/is-super-mod";
  public static final String REMOVE_MOD_PATH = "/moderator-toolbox/super-mod/remove-mod";
  public static final String ADD_SUPER_MOD_PATH = "/moderator-toolbox/super-mod/add-super-mod";
  public static final String CHECK_USER_EXISTS_PATH = "/moderator-toolbox/does-user-exist";

  public static final String SUPER_MOD_GENERATE_SINGLE_USE_KEY_PATH =
      "/moderator-toolbox/super-mod/generate-single-use-key";
  public static final String ADD_MODERATOR_PATH = "/moderator-toolbox/super-mod/add-moderator";

  private final ToolboxHttpHeaders httpHeaders;
  private final ToolboxModeratorManagementFeignClient client;

  public static ToolboxModeratorManagementClient newClient(
      final URI serverUri, final ApiKeyPassword apiKeyPassword) {
    return new ToolboxModeratorManagementClient(
        new ToolboxHttpHeaders(apiKeyPassword),
        new HttpClient<>(ToolboxModeratorManagementFeignClient.class, serverUri).get());
  }

  public List<ModeratorInfo> fetchModeratorList() {
    return client.fetchModerators(httpHeaders.createHeaders());
  }

  public boolean isCurrentUserSuperMod() {
    return client.isSuperMod(httpHeaders.createHeaders());
  }

  public void removeMod(final String moderatorName) {
    checkArgument(moderatorName != null);
    client.removeMod(httpHeaders.createHeaders(), moderatorName);
  }

  public void addSuperMod(final String moderatorName) {
    checkArgument(moderatorName != null);
    client.addSuperMod(httpHeaders.createHeaders(), moderatorName);
  }

  public boolean checkUserExists(final String userNameRequested) {
    checkArgument(userNameRequested.length() > 3);
    return client.checkUserExists(httpHeaders.createHeaders(), userNameRequested);
  }

  public String generateSingleUseKey(final String moderatorName) {
    checkArgument(moderatorName != null);
    return client.generateSingleUseKey(httpHeaders.createHeaders(), moderatorName).getApiKey();
  }

  public void addModerator(final String username) {
    checkArgument(username != null);
    client.addModerator(httpHeaders.createHeaders(), username);
  }
}
