package games.strategy.triplea.ai.pro.logging;

import games.strategy.triplea.ai.pro.ProAi;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.triplea.io.IoUtils;

/** Class to manage log settings. */
@Getter
@Setter
@Log
final class ProLogSettings implements Serializable {
  private static final long serialVersionUID = 5532153908942939829L;
  private static final String PROGRAM_SETTINGS = "Program Settings";

  private boolean logHistoryLimited = true;
  private int logHistoryLimit = 5;
  private boolean logEnabled = true;
  private Level logLevel = Level.FINEST;

  static ProLogSettings loadSettings() {
    try {
      final byte[] pool =
          Preferences.userNodeForPackage(ProAi.class).getByteArray(PROGRAM_SETTINGS, null);
      if (pool != null) {
        return IoUtils.readFromMemory(
            pool,
            is -> {
              try (ObjectInputStream ois = new ObjectInputStream(is)) {
                return (ProLogSettings) ois.readObject();
              } catch (final ClassNotFoundException e) {
                throw new IOException(e);
              }
            });
      }
    } catch (final Exception ex) {
      log.log(Level.SEVERE, "Failed to load pro AI log settings", ex);
    }

    return new ProLogSettings();
  }

  static void saveSettings(final ProLogSettings settings) {
    try {
      final byte[] bytes =
          IoUtils.writeToMemory(
              os -> {
                try (ObjectOutputStream outputStream = new ObjectOutputStream(os)) {
                  outputStream.writeObject(settings);
                }
              });
      final Preferences prefs = Preferences.userNodeForPackage(ProAi.class);
      prefs.putByteArray(PROGRAM_SETTINGS, bytes);
      try {
        prefs.flush();
      } catch (final BackingStoreException ex) {
        log.log(Level.SEVERE, "Failed to flush preferences: " + prefs.absolutePath(), ex);
      }
    } catch (final Exception ex) {
      log.log(Level.SEVERE, "Failed to save pro AI log settings", ex);
    }
  }
}
