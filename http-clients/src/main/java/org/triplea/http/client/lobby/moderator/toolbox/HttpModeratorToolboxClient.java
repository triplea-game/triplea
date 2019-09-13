package org.triplea.http.client.lobby.moderator.toolbox;

import java.net.URI;
import lombok.Getter;
import org.triplea.http.client.moderator.toolbox.access.log.ToolboxAccessLogClient;
import org.triplea.http.client.moderator.toolbox.bad.words.ToolboxBadWordsClient;
import org.triplea.http.client.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.moderator.toolbox.event.log.ToolboxEventLogClient;
import org.triplea.http.client.moderator.toolbox.moderator.management.ToolboxModeratorManagementClient;

@Getter
public class HttpModeratorToolboxClient {

  private final ToolboxAccessLogClient toolboxAccessLogClient;
  private final ToolboxUserBanClient toolboxUserBanClient;
  private final ToolboxUsernameBanClient toolboxUsernameBanClient;
  private final ToolboxModeratorManagementClient toolboxModeratorManagementClient;
  private final ToolboxBadWordsClient toolboxBadWordsClient;
  private final ToolboxEventLogClient toolboxEventLogClient;

  public HttpModeratorToolboxClient(final URI lobbyUri, final String apiKey) {
    toolboxAccessLogClient = ToolboxAccessLogClient.newClient(lobbyUri, apiKey);
    toolboxUserBanClient = ToolboxUserBanClient.newClient(lobbyUri, apiKey);
    toolboxUsernameBanClient = ToolboxUsernameBanClient.newClient(lobbyUri, apiKey);
    toolboxModeratorManagementClient = ToolboxModeratorManagementClient.newClient(lobbyUri, apiKey);
    toolboxBadWordsClient = ToolboxBadWordsClient.newClient(lobbyUri, apiKey);
    toolboxEventLogClient = ToolboxEventLogClient.newClient(lobbyUri, apiKey);
  }
}
