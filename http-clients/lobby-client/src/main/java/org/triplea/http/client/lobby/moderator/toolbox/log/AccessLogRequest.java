package org.triplea.http.client.lobby.moderator.toolbox.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessLogRequest {
  private AccessLogSearchRequest accessLogSearchRequest;
  private PagingParams pagingParams;
}
