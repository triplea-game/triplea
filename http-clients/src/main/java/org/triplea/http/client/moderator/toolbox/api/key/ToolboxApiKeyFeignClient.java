package org.triplea.http.client.moderator.toolbox.api.key;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;
import org.triplea.http.client.moderator.toolbox.NewApiKey;

interface ToolboxApiKeyFeignClient {
  @RequestLine("POST " + ToolboxApiKeyClient.VALIDATE_API_KEY_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void validateApiKey(@HeaderMap Map<String, Object> headerMap);

  @RequestLine("POST " + ToolboxApiKeyClient.GENERATE_SINGLE_USE_KEY_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  NewApiKey generateSingleUseKey(@HeaderMap Map<String, Object> headers);

  @RequestLine("GET " + ToolboxApiKeyClient.GET_API_KEYS)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  List<ApiKeyData> getApiKeys(@HeaderMap Map<String, Object> headers);

  @RequestLine("POST " + ToolboxApiKeyClient.DELETE_API_KEY)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void deleteApiKey(@HeaderMap Map<String, Object> headers, String keyId);

  @RequestLine("GET " + ToolboxApiKeyClient.RESET_LOCKOUTS_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void clearLockouts();
}
