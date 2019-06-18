package org.triplea.http.client.moderator.toolbox.banned.user;

import java.net.URI;
import java.util.List;

import org.triplea.http.client.HttpClient;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Http client object for adding, removing and querying server for user bans. User bans are done
 * by the network identifiers of a user, the user name is for informational purposes only.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxUserBanClient {
  public static final String GET_USER_BANS_PATH = "/moderator-toolbox/get-user-bans";
  public static final String REMOVE_USER_BAN_PATH = "/moderator-toolbox/remove-user-ban";
  public static final String BAN_USER_PATH = "/moderator-toolbox/ban-user";

  private final ToolboxHttpHeaders toolboxHttpHeaders;
  private final ToolboxUserBanFeignClient client;

  public static ToolboxUserBanClient newClient(final URI serverUri, final ApiKeyPassword apiKeyPassword) {
    return new ToolboxUserBanClient(
        new ToolboxHttpHeaders(apiKeyPassword),
        new HttpClient<>(ToolboxUserBanFeignClient.class, serverUri).get());
  }

  public List<UserBanData> getUserBans() {
    return client.getUserBans(toolboxHttpHeaders.createHeaders());
  }

  public void removeUserBan(final String banId) {
    client.removeUserBan(toolboxHttpHeaders.createHeaders(), banId);
  }

  public void banUser(final UserBanParams banUserParams) {
    client.banUser(toolboxHttpHeaders.createHeaders(), banUserParams);
  }
}
