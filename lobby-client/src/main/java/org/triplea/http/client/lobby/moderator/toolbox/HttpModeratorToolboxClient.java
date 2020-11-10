package org.triplea.http.client.lobby.moderator.toolbox;

import java.net.URI;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxAccessLogClient;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxEventLogClient;
import org.triplea.http.client.lobby.moderator.toolbox.management.ToolboxModeratorManagementClient;
import org.triplea.http.client.lobby.moderator.toolbox.words.ToolboxBadWordsClient;

@Getter
public class HttpModeratorToolboxClient {

  private final ToolboxAccessLogClient toolboxAccessLogClient;
  private final ToolboxUserBanClient toolboxUserBanClient;
  private final ToolboxUsernameBanClient toolboxUsernameBanClient;
  private final ToolboxModeratorManagementClient toolboxModeratorManagementClient;
  private final ToolboxBadWordsClient toolboxBadWordsClient;
  private final ToolboxEventLogClient toolboxEventLogClient;

  private HttpModeratorToolboxClient(final URI lobbyUri, final ApiKey apiKey) {
    toolboxAccessLogClient = ToolboxAccessLogClient.newClient(lobbyUri, apiKey);
    toolboxUserBanClient = ToolboxUserBanClient.newClient(lobbyUri, apiKey);
    toolboxUsernameBanClient = ToolboxUsernameBanClient.newClient(lobbyUri, apiKey);
    toolboxModeratorManagementClient = ToolboxModeratorManagementClient.newClient(lobbyUri, apiKey);
    toolboxBadWordsClient = ToolboxBadWordsClient.newClient(lobbyUri, apiKey);
    toolboxEventLogClient = ToolboxEventLogClient.newClient(lobbyUri, apiKey);
  }

  public static HttpModeratorToolboxClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return new HttpModeratorToolboxClient(lobbyUri, apiKey);
  }
}
