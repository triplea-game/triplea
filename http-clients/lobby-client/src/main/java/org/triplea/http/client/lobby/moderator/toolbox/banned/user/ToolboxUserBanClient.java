package org.triplea.http.client.lobby.moderator.toolbox.banned.user;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServerPaths;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Http client object for adding, removing and querying server for user bans. User bans are done by
 * the network identifiers of a user, the user name is for informational purposes only.
 */
public interface ToolboxUserBanClient {

  static ToolboxUserBanClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ToolboxUserBanClient.class, serverUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("GET " + ServerPaths.GET_USER_BANS_PATH)
  List<UserBanData> getUserBans();

  @RequestLine("POST " + ServerPaths.REMOVE_USER_BAN_PATH)
  void removeUserBan(String banId);

  @RequestLine("POST " + ServerPaths.BAN_USER_PATH)
  void banUser(UserBanParams banUserParams);
}
