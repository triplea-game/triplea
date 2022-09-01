package org.triplea.http.client.lobby;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.LobbyHttpClientConfig;

/** Small class to encapsulate api key and create http Authorization header. */
@AllArgsConstructor
public class AuthenticationHeaders {
  public static final String API_KEY_HEADER = "Authorization";
  public static final String KEY_BEARER_PREFIX = "Bearer";
  public static final String SYSTEM_ID_HEADER = "System-Id-Header";

  private final ApiKey apiKey;

  /** Creates headers containing both an API-Key and a 'System-Id'. */
  public Map<String, String> createHeaders() {
    final Map<String, String> headerMap = new HashMap<>();
    headerMap.put(API_KEY_HEADER, KEY_BEARER_PREFIX + " " + apiKey);
    headerMap.putAll(systemIdHeaders());
    return headerMap;
  }

  /** Creates headers containing 'System-Id' only. */
  public static Map<String, String> systemIdHeaders() {
    final Map<String, String> headerMap = new HashMap<>();
    headerMap.put("Triplea-Version", LobbyHttpClientConfig.getConfig().getClientVersion());
    headerMap.put(SYSTEM_ID_HEADER, LobbyHttpClientConfig.getConfig().getSystemId());
    return headerMap;
  }
}
