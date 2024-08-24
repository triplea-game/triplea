package org.triplea.domain.data;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

/** Loads a SystemId from persistence. */
@UtilityClass
@Slf4j
public class SystemIdLoader {
  @NonNls private static final String SYSTEM_KEY = "system-id-key";

  @Setter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  private static PreferencesPersistence preferencesPersistence =
      new PreferencesPersistence() {
        @Override
        public void save(final String value) {
          Preferences.userNodeForPackage(SystemIdLoader.class).put(SYSTEM_KEY, value);
          try {
            Preferences.userNodeForPackage(SystemIdLoader.class).flush();
          } catch (final BackingStoreException e) {
            log.error("Failed to persist system id", e);
          }
        }

        @Nullable
        @Override
        public String get() {
          return Preferences.userNodeForPackage(SystemIdLoader.class).get(SYSTEM_KEY, null);
        }
      };

  public static SystemId load() {

    final String systemId =
        Optional.ofNullable(preferencesPersistence.get())
            .orElseGet(
                () -> {
                  final String uuid = UUID.randomUUID().toString();
                  preferencesPersistence.save(uuid);
                  return uuid;
                });
    return SystemId.of(systemId);
  }

  interface PreferencesPersistence {
    void save(String value);

    @Nullable
    String get();
  }
}
