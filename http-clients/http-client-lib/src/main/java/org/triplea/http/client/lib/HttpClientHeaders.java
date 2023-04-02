package org.triplea.http.client.lib;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.SystemIdLoader;

/** Small class to encapsulate api key and create http Authorization header. */
@Builder
public class HttpClientHeaders {
  public static final String VERSION_HEADER = "Triplea-Version";
  public static final String SYSTEM_ID_HEADER = "System-Id";

  @Nonnull private final String clientVersion;
  private final String apiKey;
  private static final String systemId = SystemIdLoader.load().getValue();

  public static Map<String, String> defaultHeadersWithClientVersion(String clientVersion) {
    return builder().clientVersion(clientVersion).build().createHeaders();
  }

  /** Creates headers containing 'System-Id' only. */
  public Map<String, String> createHeaders() {
    final Map<String, String> headerMap = new HashMap<>();
    headerMap.put(VERSION_HEADER, clientVersion);
    headerMap.put(SYSTEM_ID_HEADER, systemId);
    if (apiKey != null) {
      headerMap.put("Authorization", "Bearer " + apiKey);
    }
    return headerMap;
  }
}
