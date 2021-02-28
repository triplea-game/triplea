package org.triplea.http.client;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;

/** Small class to encapsulate api key and create http Authorization header. */
@AllArgsConstructor
public class AuthenticationHeaders {
  public static final String API_KEY_HEADER = "Authorization";
  public static final String KEY_BEARER_PREFIX = "Bearer";

  private final ApiKey apiKey;

  public Map<String, Object> createHeaders() {
    final Map<String, Object> headerMap = new HashMap<>();
    headerMap.put(API_KEY_HEADER, KEY_BEARER_PREFIX + " " + apiKey);
    headerMap.putAll(SystemIdHeader.headers());
    return headerMap;
  }
}
