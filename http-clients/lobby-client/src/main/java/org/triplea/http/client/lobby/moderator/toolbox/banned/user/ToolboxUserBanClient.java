package org.triplea.http.client.lobby.moderator.toolbox.banned.user;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Http client object for adding, removing and querying server for user bans. User bans are done by
 * the network identifiers of a user, the user name is for informational purposes only.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxUserBanClient {
  public static final String GET_USER_BANS_PATH = "/moderator-toolbox/get-user-bans";
  public static final String REMOVE_USER_BAN_PATH = "/moderator-toolbox/remove-user-ban";
  public static final String BAN_USER_PATH = "/moderator-toolbox/ban-user";

  private final ToolboxUserBanFeignClient client;

  public static ToolboxUserBanClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new ToolboxUserBanClient(
        HttpClient.newClient(
            ToolboxUserBanFeignClient.class,
            serverUri,
            new AuthenticationHeaders(apiKey).createHeaders()));
  }

  public List<UserBanData> getUserBans() {
    return client.getUserBans();
  }

  public void removeUserBan(final String banId) {
    client.removeUserBan(banId);
  }

  public void banUser(final UserBanParams banUserParams) {
    client.banUser(banUserParams);
  }
}
