package games.strategy.triplea.ai.proAI.logging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import games.strategy.debug.ClientLogger;
import games.strategy.triplea.ai.proAI.ProAI;

/**
 * Class to manage log settings.
 */
public class ProLogSettings implements Serializable {
  private static final long serialVersionUID = 2696071717784800413L;
  public boolean LimitLogHistory = true;
  public int LimitLogHistoryTo = 5;
  public boolean EnableAILogging = true;
  public Level AILoggingDepth = Level.FINEST;
  private static ProLogSettings s_lastSettings = null;
  private static String PROGRAM_SETTINGS = "Program Settings";

  public static ProLogSettings loadSettings() {
    if (s_lastSettings == null) {
      ProLogSettings result = new ProLogSettings();
      try {
        final byte[] pool = Preferences.userNodeForPackage(ProAI.class).getByteArray(PROGRAM_SETTINGS, null);
        if (pool != null) {
          result = (ProLogSettings) new ObjectInputStream(new ByteArrayInputStream(pool)).readObject();
        }
      } catch (final Exception ex) {
        ClientLogger.logQuietly(ex);
      }
      if (result == null) {
        result = new ProLogSettings();
      }
      s_lastSettings = result;
      return result;
    } else {
      return s_lastSettings;
    }
  }

  public static void saveSettings(final ProLogSettings settings) {
    s_lastSettings = settings;
    try (final ByteArrayOutputStream pool = new ByteArrayOutputStream(10000);
        ObjectOutputStream outputStream = new ObjectOutputStream(pool);) {

      outputStream.writeObject(settings);
      final Preferences prefs = Preferences.userNodeForPackage(ProAI.class);
      prefs.putByteArray(PROGRAM_SETTINGS, pool.toByteArray());
      try {
        prefs.flush();
      } catch (final BackingStoreException ex) {
        ClientLogger.logQuietly(ex);
      }
    } catch (final Exception ex) {
      ClientLogger.logQuietly(ex);
    }
  }
}
