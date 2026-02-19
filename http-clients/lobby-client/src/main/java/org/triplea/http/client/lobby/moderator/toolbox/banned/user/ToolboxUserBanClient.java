package org.triplea.http.client.lobby.moderator.toolbox.banned.user;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Http client object for adding, removing and querying server for user bans. User bans are done by
 * the network identifiers of a user, the user name is for informational purposes only.
 */
public interface ToolboxUserBanClient {
  String GET_USER_BANS_PATH = "/lobby/moderator-toolbox/get-user-bans";
  String REMOVE_USER_BAN_PATH = "/lobby/moderator-toolbox/remove-user-ban";
  String BAN_USER_PATH = "/lobby/moderator-toolbox/ban-user";

  static ToolboxUserBanClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ToolboxUserBanClient.class, serverUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("GET " + ToolboxUserBanClient.GET_USER_BANS_PATH)
  List<UserBanData> getUserBans();

  @RequestLine("POST " + ToolboxUserBanClient.REMOVE_USER_BAN_PATH)
  void removeUserBan(String banId);

  @RequestLine("POST " + ToolboxUserBanClient.BAN_USER_PATH)
  void banUser(UserBanParams banUserParams);
}
