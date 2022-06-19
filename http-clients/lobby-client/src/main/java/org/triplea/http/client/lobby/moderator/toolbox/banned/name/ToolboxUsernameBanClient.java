package org.triplea.http.client.lobby.moderator.toolbox.banned.name;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/** Http client object for adding, removing and querying server for user name bans. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxUsernameBanClient {
  public static final String REMOVE_BANNED_USER_NAME_PATH =
      "/moderator-toolbox/remove-username-ban";
  public static final String ADD_BANNED_USER_NAME_PATH = "/moderator-toolbox/add-username-ban";
  public static final String GET_BANNED_USER_NAMES_PATH = "/moderator-toolbox/get-username-bans";

  private final ToolboxUsernameBanFeignClient client;

  public static ToolboxUsernameBanClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new ToolboxUsernameBanClient(
        HttpClient.newClient(
            ToolboxUsernameBanFeignClient.class,
            serverUri,
            new AuthenticationHeaders(apiKey).createHeaders()));
  }

  public void removeUsernameBan(final String username) {
    checkArgument(username != null);
    client.removeUsernameBan(username);
  }

  public void addUsernameBan(final String username) {
    client.addUsernameBan(username);
  }

  public List<UsernameBanData> getUsernameBans() {
    return client.getUsernameBans();
  }
}
