package org.triplea.http.client.moderator.toolbox;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;

/** Small class to encapsulate api key password (key+password) and create http headers. */
@AllArgsConstructor
public class ToolboxHttpHeaders {
  public static final String API_KEY_HEADER = "Authorization";
  public static final String KEY_BEARER_PREFIX = "Bearer";

  private final String apiKey;

  public Map<String, Object> createHeaders() {
    final Map<String, Object> headerMap = new HashMap<>();
    headerMap.put(API_KEY_HEADER, KEY_BEARER_PREFIX + " " + apiKey);
    return headerMap;
  }
}
