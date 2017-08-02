package games.strategy.engine.framework.lookandfeel;

import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingAction;

public class LookAndFeel {

  public static List<String> getLookAndFeelAvailableList() {
    final List<String> substanceLooks = new ArrayList<>();
    for (final UIManager.LookAndFeelInfo look : UIManager.getInstalledLookAndFeels()) {
      substanceLooks.add(look.getClassName());
    }
    return substanceLooks;
  }

  public static void setupLookAndFeel() {
    SwingAction.invokeAndWait(() -> {
      try {
        UIManager.setLookAndFeel(ClientSetting.LOOK_AND_FEEL_PREF.value());
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
            ClientLogger.logQuietly(e);
          }
        }
      }
    });
  }

}
