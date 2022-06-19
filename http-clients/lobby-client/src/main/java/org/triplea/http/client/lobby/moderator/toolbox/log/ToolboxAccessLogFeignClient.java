package org.triplea.http.client.lobby.moderator.toolbox.log;

import feign.RequestLine;
import java.util.List;

interface ToolboxAccessLogFeignClient {
  @RequestLine("POST " + ToolboxAccessLogClient.FETCH_ACCESS_LOG_PATH)
  List<AccessLogData> getAccessLog(AccessLogRequest accessLogRequest);
}
