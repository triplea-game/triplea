package games.strategy.engine.framework.lookandfeel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.UIManager;

import org.pushingpixels.substance.api.skin.SubstanceAutumnLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceBusinessBlackSteelLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceBusinessLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceCeruleanLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceCremeCoffeeLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceCremeLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceDustCoffeeLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceDustLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGeminiLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteChalkLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteGoldLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceMagellanLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceMarinerLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceMistAquaLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceNebulaBrickWallLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceOfficeBlack2007LookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceSaharaLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceTwilightLookAndFeel;

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
    substanceLooks.addAll(Arrays.asList(SubstanceAutumnLookAndFeel.class.getName(),
        SubstanceBusinessBlackSteelLookAndFeel.class.getName(), SubstanceBusinessBlueSteelLookAndFeel.class.getName(),
        SubstanceBusinessLookAndFeel.class.getName(), SubstanceCeruleanLookAndFeel.class.getName(),
        SubstanceCremeCoffeeLookAndFeel.class.getName(), SubstanceCremeLookAndFeel.class.getName(),
        SubstanceDustCoffeeLookAndFeel.class.getName(), SubstanceDustLookAndFeel.class.getName(),
        SubstanceGeminiLookAndFeel.class.getName(), SubstanceGraphiteAquaLookAndFeel.class.getName(),
        SubstanceGraphiteChalkLookAndFeel.class.getName(), SubstanceGraphiteGlassLookAndFeel.class.getName(),
        SubstanceGraphiteGoldLookAndFeel.class.getName(), SubstanceGraphiteLookAndFeel.class.getName(),
        SubstanceMagellanLookAndFeel.class.getName(), SubstanceMarinerLookAndFeel.class.getName(),
        SubstanceMistAquaLookAndFeel.class.getName(), SubstanceMistSilverLookAndFeel.class.getName(),
        SubstanceModerateLookAndFeel.class.getName(), SubstanceNebulaBrickWallLookAndFeel.class.getName(),
        SubstanceNebulaLookAndFeel.class.getName(), SubstanceOfficeBlack2007LookAndFeel.class.getName(),
        SubstanceOfficeBlue2007LookAndFeel.class.getName(), SubstanceOfficeSilver2007LookAndFeel.class.getName(),
        SubstanceRavenLookAndFeel.class.getName(), SubstanceSaharaLookAndFeel.class.getName(),
        SubstanceTwilightLookAndFeel.class.getName()));
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
            ClientLogger.logQuietly("Failed to set system look and feel", e);
          }
        }
      }
    });
  }

}
