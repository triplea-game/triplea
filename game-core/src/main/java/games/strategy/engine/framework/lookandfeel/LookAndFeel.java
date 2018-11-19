package games.strategy.engine.framework.lookandfeel;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.triplea.common.util.Services;
import org.triplea.game.client.ui.swing.laf.SubstanceLookAndFeelManager;

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.SettingsWindow;
import lombok.extern.java.Log;

/**
 * Provides methods for working with the Swing Look-And-Feel.
 */
@Log
public final class LookAndFeel {
  private LookAndFeel() {}

  /**
   * Initializes the Swing Look-And-Feel subsystem.
   *
   * <p>
   * Sets the user's preferred Look-And-Feel. If not available, the system Look-And-Feel will be used.
   * </p>
   * <p>
   * This method must be called before creating the first UI component but after initializing the client settings
   * framework.
   * </p>
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   */
  public static void initialize() {
    Services.loadAny(SubstanceLookAndFeelManager.class).initialize();
    ClientSetting.lookAndFeel.addListener(gameSetting -> {
      setupLookAndFeel(gameSetting.getValueOrThrow());
      SettingsWindow.updateLookAndFeel();
      JOptionPane.showMessageDialog(
          null,
          "Look and feel changes can cause instability.\n"
              + "Please restart all running TripleA instances.",
          "Close TripleA and Restart",
          JOptionPane.WARNING_MESSAGE);
    });
    setupLookAndFeel(ClientSetting.lookAndFeel.getValueOrThrow());
  }

  /**
   * Returns a collection of the available Look-And-Feels.
   */
  public static List<String> getLookAndFeelAvailableList() {
    final List<String> lookAndFeelClassNames = new ArrayList<>();
    lookAndFeelClassNames.addAll(getInstalledLookAndFeelClassNames());
    lookAndFeelClassNames.addAll(getSubstanceLookAndFeelClassNames());
    return lookAndFeelClassNames;
  }

  private static Collection<String> getInstalledLookAndFeelClassNames() {
    return Arrays.stream(UIManager.getInstalledLookAndFeels())
        .map(UIManager.LookAndFeelInfo::getClassName)
        .collect(Collectors.toList());
  }

  private static Collection<String> getSubstanceLookAndFeelClassNames() {
    return Arrays.asList(
        "Autumn",
        "BusinessBlackSteel",
        "BusinessBlueSteel",
        "Business",
        "Cerulean",
        "CremeCoffee",
        "Creme",
        "DustCoffee",
        "Dust",
        "Gemini",
        "GraphiteAqua",
        "GraphiteChalk",
        "GraphiteGlass",
        "GraphiteGold",
        "Graphite",
        "Magellan",
        "Mariner",
        "MistAqua",
        "MistSilver",
        "Moderate",
        "NebulaBrickWall",
        "Nebula",
        "OfficeBlack2007",
        "OfficeBlue2007",
        "OfficeSilver2007",
        "Raven",
        "Sahara",
        "Twilight").stream()
        .map(LookAndFeel::substance)
        .collect(Collectors.toList());
  }

  private static String substance(final String baseName) {
    return "org.pushingpixels.substance.api.skin.Substance" + baseName + "LookAndFeel";
  }

  public static String getDefaultLookAndFeelClassName() {
    return SystemProperties.isMac() ? UIManager.getSystemLookAndFeelClassName() : substance("Graphite");
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
