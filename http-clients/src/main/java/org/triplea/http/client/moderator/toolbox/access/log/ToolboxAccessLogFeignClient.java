package org.triplea.http.client.moderator.toolbox.access.log;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;
import org.triplea.http.client.moderator.toolbox.PagingParams;

interface ToolboxAccessLogFeignClient {
  @RequestLine("POST " + ToolboxAccessLogClient.FETCH_ACCESS_LOG_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  List<AccessLogData> getAccessLog(
      @HeaderMap Map<String, Object> headers, PagingParams pagingParams);
}
