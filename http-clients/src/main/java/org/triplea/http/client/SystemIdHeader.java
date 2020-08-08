package org.triplea.http.client;

import com.google.common.annotations.VisibleForTesting;
import java.util.Map;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.SystemIdLoader;

/**
 * Creates headers with a user's 'system-id'. If a system-id has not been generated, one will be
 * generated and stored in preferences.
 */
@UtilityClass
public class SystemIdHeader {
  public static final String SYSTEM_ID_HEADER = "System-Id-Header";

  public static Map<String, Object> headers() {
    return headers(SystemIdLoader::load);
  }

  @VisibleForTesting
  static Map<String, Object> headers(final Supplier<SystemId> systemIdSupplier) {
    return Map.of(SYSTEM_ID_HEADER, systemIdSupplier.get().getValue());
  }
}
