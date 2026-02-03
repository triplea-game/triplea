package org.triplea.http.client.lobby.moderator.toolbox.log;

import static com.google.common.base.Preconditions.checkArgument;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;

/**
 * Http client class for fetching rows of the access log table. That is a table noting users that
 * have accessed the lobby. The data is useful for informative reasons and provides the key
 * parameters for adding user name or user bans.
 */
public interface ToolboxAccessLogClient {
  String FETCH_ACCESS_LOG_PATH = "/lobby/moderator-toolbox/access-log";

  static ToolboxAccessLogClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ToolboxAccessLogClient.class, serverUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + ToolboxAccessLogClient.FETCH_ACCESS_LOG_PATH)
  List<AccessLogData> getAccessLog(AccessLogRequest accessLogRequest);

  default List<AccessLogData> getAccessLog(
      final AccessLogSearchRequest searchRequest, final PagingParams pagingParams) {
    checkArgument(pagingParams.getRowNumber() >= 0);
    checkArgument(pagingParams.getPageSize() > 0);
    return getAccessLog(
        AccessLogRequest.builder()
            .accessLogSearchRequest(searchRequest)
            .pagingParams(pagingParams)
            .build());
  }

  default List<AccessLogData> getAccessLog(final PagingParams pagingParams) {
    return getAccessLog(AccessLogSearchRequest.EMPTY_SEARCH, pagingParams);
  }
}
