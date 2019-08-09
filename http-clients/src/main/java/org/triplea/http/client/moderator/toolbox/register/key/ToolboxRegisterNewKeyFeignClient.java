package org.triplea.http.client.moderator.toolbox.register.key;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

interface ToolboxRegisterNewKeyFeignClient {
  @RequestLine("POST " + ToolboxRegisterNewKeyClient.REGISTER_API_KEY_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  RegisterApiKeyResult registerKey(@HeaderMap Map<String, Object> headers);
}
