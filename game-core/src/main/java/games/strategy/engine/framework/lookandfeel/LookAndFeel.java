package games.strategy.engine.framework.lookandfeel;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
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

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.SettingsWindow;
import lombok.extern.java.Log;

/**
 * Provides methods for working with the Swing Look-And-Feel.
 */
@Log
public final class LookAndFeel {
  static {
    ClientSetting.LOOK_AND_FEEL_PREF.addSaveListener(newValue -> {
      setupLookAndFeel(newValue);
      SettingsWindow.updateLookAndFeel();
      JOptionPane.showMessageDialog(
          null,
          "Look and feel changes can cause instability.\n"
              + "Please restart all running TripleA instances.",
          "Close TripleA and Restart",
          JOptionPane.WARNING_MESSAGE);
    });
  }

  private LookAndFeel() {}

  /**
   * Returns a collection of the available Look-And-Feels.
   */
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

  /**
   * Sets the user's preferred Look-And-Feel. If not available, the system Look-And-Feel will be used.
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   */
  public static void setupLookAndFeel() {
    setupLookAndFeel(ClientSetting.LOOK_AND_FEEL_PREF.value());
  }

  private static void setupLookAndFeel(final String lookAndFeelName) {
    checkState(SwingUtilities.isEventDispatchThread());

    // On Mac, have the menubar appear at the top of the screen to match how Mac apps are expected to behave.
    if (SystemProperties.isMac()) {
      System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    try {
      UIManager.setLookAndFeel(lookAndFeelName);
    } catch (final Throwable t) {
      if (!SystemProperties.isMac()) {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
          log.log(Level.SEVERE, "Failed to set system look and feel", e);
        }
      }
    }
  }
}
