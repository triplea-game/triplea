package org.triplea.http.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;

@Builder
public class ClientIdentifiers {

  @Nonnull private final String applicationVersion;
  @Nonnull private final String systemId;
  @Nullable private final String apiKey;

  /** Creates headers containing 'System-Id' only. */
  public Map<String, String> createHeaders() {
    final Map<String, String> headerMap = new HashMap<>();
    headerMap.put(HttpHeaders.VERSION_HEADER, applicationVersion);
    headerMap.put(HttpHeaders.SYSTEM_ID_HEADER, systemId);
    Optional.ofNullable(apiKey)
        .ifPresent(apiKey -> headerMap.put("Authorization", "Bearer " + apiKey));
    return headerMap;
  }
}
