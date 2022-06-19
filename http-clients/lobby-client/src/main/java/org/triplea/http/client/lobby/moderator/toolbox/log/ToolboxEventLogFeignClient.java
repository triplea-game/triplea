package org.triplea.http.client.lobby.moderator.toolbox.log;

import feign.RequestLine;
import java.util.List;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;

interface ToolboxEventLogFeignClient {
  @RequestLine("POST " + ToolboxEventLogClient.AUDIT_HISTORY_PATH)
  List<ModeratorEvent> lookupModeratorEvents(PagingParams params);
}
