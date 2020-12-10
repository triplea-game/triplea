package org.triplea.http.client.lobby.moderator.toolbox.log;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface ToolboxAccessLogFeignClient {
  @RequestLine("POST " + ToolboxAccessLogClient.FETCH_ACCESS_LOG_PATH)
  List<AccessLogData> getAccessLog(
      @HeaderMap Map<String, Object> headers, AccessLogRequest accessLogRequest);
}
