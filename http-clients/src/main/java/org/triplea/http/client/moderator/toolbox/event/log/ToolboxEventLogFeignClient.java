package org.triplea.http.client.moderator.toolbox.event.log;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;
import org.triplea.http.client.moderator.toolbox.PagingParams;

interface ToolboxEventLogFeignClient {
  @RequestLine("POST " + ToolboxEventLogClient.AUDIT_HISTORY_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  List<ModeratorEvent> lookupModeratorEvents(
      @HeaderMap Map<String, Object> headerMap, PagingParams params);
}
