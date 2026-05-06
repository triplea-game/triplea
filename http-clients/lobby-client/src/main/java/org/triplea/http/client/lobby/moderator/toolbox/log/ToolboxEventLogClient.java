package org.triplea.http.client.lobby.moderator.toolbox.log;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServerPaths;
import org.triplea.http.client.lobby.AuthenticationHeaders;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;

/**
 * Client object to query server for the moderator event log. The event log is an audit trail of
 * moderator actions.
 */
public interface ToolboxEventLogClient {

  static ToolboxEventLogClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ToolboxEventLogClient.class, serverUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  /** Method to lookup moderator audit history events with paging. */
  @RequestLine("POST " + ServerPaths.AUDIT_HISTORY_PATH)
  List<ModeratorEvent> lookupModeratorEvents(PagingParams params);
}
