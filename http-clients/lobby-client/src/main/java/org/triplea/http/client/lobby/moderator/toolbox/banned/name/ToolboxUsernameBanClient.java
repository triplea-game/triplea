package org.triplea.http.client.lobby.moderator.toolbox.banned.name;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServerPaths;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/** Http client object for adding, removing and querying server for user name bans. */
public interface ToolboxUsernameBanClient {

  static ToolboxUsernameBanClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ToolboxUsernameBanClient.class,
        serverUri,
        new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + ServerPaths.REMOVE_BANNED_USER_NAME_PATH)
  void removeUsernameBan(String username);

  @RequestLine("POST " + ServerPaths.ADD_BANNED_USER_NAME_PATH)
  void addUsernameBan(String username);

  @RequestLine("GET " + ServerPaths.GET_BANNED_USER_NAMES_PATH)
  List<UsernameBanData> getUsernameBans();
}
