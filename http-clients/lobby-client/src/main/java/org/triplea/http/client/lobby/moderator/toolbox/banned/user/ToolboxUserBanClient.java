package org.triplea.http.client.lobby.moderator.toolbox.banned.user;

import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/**
 * Http client object for adding, removing and querying server for user bans. User bans are done by
 * the network identifiers of a user, the user name is for informational purposes only.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxUserBanClient {
  public static final String GET_USER_BANS_PATH = "/moderator-toolbox/get-user-bans";
  public static final String REMOVE_USER_BAN_PATH = "/moderator-toolbox/remove-user-ban";
  public static final String BAN_USER_PATH = "/moderator-toolbox/ban-user";

  private final Map<String, Object> authenticationHeaders;
  private final ToolboxUserBanFeignClient client;

  public static ToolboxUserBanClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new ToolboxUserBanClient(
        new AuthenticationHeaders(apiKey).createHeaders(),
        new HttpClient<>(ToolboxUserBanFeignClient.class, serverUri).get());
  }

  public List<UserBanData> getUserBans() {
    return client.getUserBans(authenticationHeaders);
  }

  public void removeUserBan(final String banId) {
    client.removeUserBan(authenticationHeaders, banId);
  }

  public void banUser(final UserBanParams banUserParams) {
    client.banUser(authenticationHeaders, banUserParams);
  }
}
