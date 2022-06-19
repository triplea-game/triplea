package org.triplea.http.client.lobby.moderator.toolbox.log;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;

/**
 * Client object to query server for the moderator event log. The event log is an audit trail of
 * moderator actions.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxEventLogClient {
  public static final String AUDIT_HISTORY_PATH = "/moderator-toolbox/audit-history/lookup";

  private final ToolboxEventLogFeignClient client;

  public static ToolboxEventLogClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new ToolboxEventLogClient(
        HttpClient.newClient(
            ToolboxEventLogFeignClient.class,
            serverUri,
            new AuthenticationHeaders(apiKey).createHeaders()));
  }

  /** Method to lookup moderator audit history events with paging. */
  public List<ModeratorEvent> lookupModeratorEvents(final PagingParams pagingParams) {
    checkArgument(pagingParams.getRowNumber() >= 0);
    checkArgument(pagingParams.getPageSize() > 0);

    return client.lookupModeratorEvents(pagingParams);
  }
}
