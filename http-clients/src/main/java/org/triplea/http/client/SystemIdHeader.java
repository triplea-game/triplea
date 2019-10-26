package org.triplea.http.client;

import com.google.common.annotations.VisibleForTesting;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 * Creates headers with a user's 'system-id'. If a system-id has not been generated, one will be
 * generated and stored in preferences.
 */
@Log
@UtilityClass
public class SystemIdHeader {
  public static final String SYSTEM_ID_HEADER = "system-id-header";

  private static final String SYSTEM_KEY = "system-id-key";

  interface PreferencesPersistence {
    void save(String value);

    @Nullable
    String get();
  }

  @Setter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  private static PreferencesPersistence preferencesPersistence =
      new PreferencesPersistence() {
        @Override
        public void save(final String value) {
          Preferences.userNodeForPackage(SystemIdHeader.class).put(SYSTEM_KEY, value);
          try {
            Preferences.userNodeForPackage(SystemIdHeader.class).flush();
          } catch (final BackingStoreException e) {
            log.log(Level.SEVERE, "Failed to persist system id", e);
          }
        }

        @Nullable
        @Override
        public String get() {
          return Preferences.userNodeForPackage(SystemIdHeader.class).get(SYSTEM_KEY, null);
        }
      };

  public static Map<String, Object> headers() {
    final String systemId =
        Optional.ofNullable(preferencesPersistence.get())
            .orElseGet(
                () -> {
                  final String uuid = UUID.randomUUID().toString();
                  preferencesPersistence.save(uuid);
                  return uuid;
                });

    return Map.of(SYSTEM_ID_HEADER, systemId);
  }
}
