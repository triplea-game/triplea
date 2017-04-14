package games.strategy.engine.framework.lookandfeel;

import java.util.Arrays;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.settings.SystemPreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;
import games.strategy.triplea.ui.menubar.TripleAMenuBar;
import games.strategy.ui.SwingAction;

public class LookAndFeel {
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
    String defaultLookAndFeel = SubstanceGraphiteLookAndFeel.class.getName();

    if (SystemProperties.isMac()) {
      // stay consistent with mac look and feel if we are on a mac
      defaultLookAndFeel = UIManager.getSystemLookAndFeelClassName();
    }

    String userDefault = SystemPreferences.get(SystemPreferenceKey.LOOK_AND_FEEL_PREF, defaultLookAndFeel);
    final List<String> availableSkins = TripleAMenuBar.getLookAndFeelAvailableList();

    if (availableSkins.contains(userDefault)) {
      return userDefault;
    }
    if (availableSkins.contains(defaultLookAndFeel)) {
      setDefaultLookAndFeel(defaultLookAndFeel);
      return defaultLookAndFeel;
    }
    return UIManager.getSystemLookAndFeelClassName();
  }

  public static void setDefaultLookAndFeel(final String lookAndFeelClassName) {
    try {
      UIManager.setLookAndFeel(lookAndFeelClassName);
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
        | UnsupportedLookAndFeelException e) {
      ClientLogger.logError("Unable to load look and feel: " + lookAndFeelClassName
          + ", retaining the old look and feel. Please do not select this look and feel, it does not work."
          + " Please do report this to the developers so the look and feel can be addressed. When doing so, please"
          + " include this list of installed look and feel debug data: "
          + Arrays.asList(UIManager.getInstalledLookAndFeels()) , e);
      return;
    }
    SystemPreferences.put(SystemPreferenceKey.LOOK_AND_FEEL_PREF, lookAndFeelClassName);
  }
}
