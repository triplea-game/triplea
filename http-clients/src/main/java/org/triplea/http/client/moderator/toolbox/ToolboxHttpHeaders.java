package org.triplea.http.client.moderator.toolbox;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;

/** Small class to encapsulate api key password (key+password) and create http headers. */
@AllArgsConstructor
public class ToolboxHttpHeaders {
  public static final String API_KEY_HEADER = "Moderator-api-key";
  public static final String API_KEY_PASSWORD_HEADER = "Moderator-api-key-password";

  private final ApiKeyPassword apiKeyPassword;

  public Map<String, Object> createHeaders() {
    final Map<String, Object> headerMap = new HashMap<>();
    headerMap.put(API_KEY_HEADER, apiKeyPassword.getApiKey());
    headerMap.put(API_KEY_PASSWORD_HEADER, apiKeyPassword.getPassword());
    return headerMap;
  }
}
