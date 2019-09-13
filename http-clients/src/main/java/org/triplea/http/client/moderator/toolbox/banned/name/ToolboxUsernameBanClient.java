package org.triplea.http.client.moderator.toolbox.banned.name;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;

/** Http client object for adding, removing and querying server for user name bans. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxUsernameBanClient {
  public static final String REMOVE_BANNED_USER_NAME_PATH =
      "/moderator-toolbox/remove-username-ban";
  public static final String ADD_BANNED_USER_NAME_PATH = "/moderator-toolbox/add-username-ban";
  public static final String GET_BANNED_USER_NAMES_PATH = "/moderator-toolbox/get-username-bans";

  private final ToolboxHttpHeaders toolboxHttpHeaders;
  private final ToolboxUsernameBanFeignClient client;

  public static ToolboxUsernameBanClient newClient(final URI serverUri, final String apiKey) {
    return new ToolboxUsernameBanClient(
        new ToolboxHttpHeaders(apiKey),
        new HttpClient<>(ToolboxUsernameBanFeignClient.class, serverUri).get());
  }

  public void removeUsernameBan(final String username) {
    checkArgument(username != null);
    client.removeUsernameBan(toolboxHttpHeaders.createHeaders(), username);
  }

  public void addUsernameBan(final String username) {
    client.addUsernameBan(toolboxHttpHeaders.createHeaders(), username);
  }

  public List<UsernameBanData> getUsernameBans() {
    return client.getUsernameBans(toolboxHttpHeaders.createHeaders());
  }
}
