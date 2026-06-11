package org.triplea.http.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;

@Builder
public class ClientIdentifiers {
  /**
   * Client version controls which server we will be connecting to. This is used in NGINX routing to
   * send requests to the correct server.
   */
  public static final String CLIENT_VERSION = "2.7";

  /** The name of the HTTP header that we use to send CLIENT_VERSION */
  public static String VERSION_HEADER = "Triplea-Version";

  /** The name of the HTTP header used to send 'systemId' */
  public static final String SYSTEM_ID_HEADER = "System-Id-Header";

  @Nonnull private final String systemId;
  @Nullable private final String apiKey;

  /** Creates headers containing 'System-Id' only. */
  public Map<String, String> createHeaders() {
    final Map<String, String> headerMap = new HashMap<>();
    headerMap.put(HttpHeaders.VERSION_HEADER, CLIENT_VERSION);
    headerMap.put(HttpHeaders.SYSTEM_ID_HEADER, systemId);
    Optional.ofNullable(apiKey)
        .ifPresent(apiKey -> headerMap.put("Authorization", "Bearer " + apiKey));
    return headerMap;
  }
}
