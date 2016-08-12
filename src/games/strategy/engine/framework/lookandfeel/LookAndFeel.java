package games.strategy.engine.framework.lookandfeel;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.ui.menubar.TripleAMenuBar;
import games.strategy.ui.SwingAction;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

import javax.swing.UIManager;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class LookAndFeel {
  public static final String LOOK_AND_FEEL_PREF = "LookAndFeel";

  public static void setupLookAndFeel() {
    SwingAction.invokeAndWait(() -> {
      try {
        UIManager.setLookAndFeel(getDefaultLookAndFeel());
        // FYI if you are getting a null pointer exception in Substance, like this:
        // org.pushingpixels.substance.internal.utils.SubstanceColorUtilities
        // .getDefaultBackgroundColor(SubstanceColorUtilities.java:758)
        // Then it is because you included the swingx substance library without including swingx.
        // You can solve by including both swingx libraries or removing both,
        // or by setting the look and feel twice in a row.
      } catch (final Throwable t) {
        if (!SystemProperties.isMac()) {
          try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          } catch (final Exception e) {
          }
        }
      }
    });
  }

  private static String getDefaultLookAndFeel() {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
    // substance 7.x
    String defaultLookAndFeel = SubstanceGraphiteLookAndFeel.class.getName();
    // macs are already beautiful
    if (SystemProperties.isMac()) {
      defaultLookAndFeel = UIManager.getSystemLookAndFeelClassName();
    }
    final String userDefault = pref.get(LOOK_AND_FEEL_PREF, defaultLookAndFeel);
    final List<String> availableSkins = TripleAMenuBar.getLookAndFeelAvailableList();
    if (!availableSkins.contains(userDefault)) {
      if (!availableSkins.contains(defaultLookAndFeel)) {
        return UIManager.getSystemLookAndFeelClassName();
      }
      setDefaultLookAndFeel(defaultLookAndFeel);
      return defaultLookAndFeel;
    }
    return userDefault;
  }

  public static void setDefaultLookAndFeel(final String lookAndFeelClassName) {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
    pref.put(LOOK_AND_FEEL_PREF, lookAndFeelClassName);
    try {
      pref.sync();
    } catch (final BackingStoreException e) {
      ClientLogger.logQuietly(e);
    }
  }
}
