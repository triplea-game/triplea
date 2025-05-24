package games.strategy.engine.framework.lookandfeel;

import static com.google.common.base.Preconditions.checkState;

import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.RelationshipTypeAttachment;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.SettingsWindow;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.game.client.ui.swing.laf.SubstanceLookAndFeelManager;
import org.triplea.util.Services;

/** Provides methods for working with the Swing Look-And-Feel. */
@Slf4j
public final class LookAndFeel {
  private LookAndFeel() {}

  private static final @NonNls String DARKER_RED = "#8B0000";

  /**
   * Initializes the Swing Look-And-Feel subsystem.
   *
   * <p>Sets the user's preferred Look-And-Feel. If not available, the system Look-And-Feel will be
   * used.
   *
   * <p>This method must be called before creating the first UI component but after initializing the
   * client settings framework.
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   */
  public static void initialize() {
    getSubstanceLookAndFeelManager().ifPresent(SubstanceLookAndFeelManager::initialize);
    ClientSetting.lookAndFeel.addListener(
        gameSetting -> {
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

  private static Optional<SubstanceLookAndFeelManager> getSubstanceLookAndFeelManager() {
    return Services.tryLoadAny(SubstanceLookAndFeelManager.class);
  }

  public static Collection<UIManager.LookAndFeelInfo> getAvailableLookAndFeels() {
    final Collection<UIManager.LookAndFeelInfo> lookAndFeels = new ArrayList<>();
    lookAndFeels.addAll(getSystemLookAndFeels());
    lookAndFeels.addAll(getSubstanceLookAndFeels());
    return lookAndFeels;
  }

  private static Collection<UIManager.LookAndFeelInfo> getSystemLookAndFeels() {
    return List.of(UIManager.getInstalledLookAndFeels());
  }

  private static Collection<UIManager.LookAndFeelInfo> getSubstanceLookAndFeels() {
    return getSubstanceLookAndFeelManager()
        .map(SubstanceLookAndFeelManager::getInstalledLookAndFeels)
        .orElseGet(List::of);
  }

  public static String getDefaultLookAndFeelClassName() {
    return SystemProperties.isMac()
        ? UIManager.getSystemLookAndFeelClassName()
        : getSubstanceLookAndFeelManager()
            .map(SubstanceLookAndFeelManager::getDefaultLookAndFeelClassName)
            .orElseGet(UIManager::getSystemLookAndFeelClassName);
  }

  public static @Nonnull @NonNls String getLookAndFeelColorRed() {
    // On a dark theme, use red. Use a darker red with a light theme.
    return LookAndFeel.isCurrentLookAndFeelDark() ? "red" : DARKER_RED;
  }

  public static boolean isCurrentLookAndFeelDark() {
    final Color background = UIManager.getColor("Panel.background");
    return background != null && isColorDark(background);
  }

  public static boolean isColorDark(final Color color) {
    // From the Wikipedia definition: https://en.wikipedia.org/wiki/Luma_%28video%29
    final double luma =
        (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
    return luma < 0.5;
  }

  private static void setupLookAndFeel(final String lookAndFeelName) {
    checkState(SwingUtilities.isEventDispatchThread());

    // On Mac, have the menu bar appear at the top of the screen to match how Mac apps are expected
    // to behave.
    if (SystemProperties.isMac()) {
      System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    try {
      UIManager.setLookAndFeel(lookAndFeelName);
    } catch (RuntimeException
        | ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | UnsupportedLookAndFeelException runtimeException) {
      if (!SystemProperties.isMac()) {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
          log.error("Failed to set system look and feel", e);
        }
      }
    }
  }

  public static @NonNls String convertColorToHex(Color color) {
    return "#" + Integer.toHexString(color.getRGB()).substring(2);
  }

  /**
   * Returns a color to represent the relationship.
   *
   * @param relType which relationship to get the color for
   * @return the color to represent this relationship
   */
  public static Color getRelationshipTypeColor(final RelationshipType relType) {
    return getRelationshipTypeAttachmentColor(relType.getRelationshipTypeAttachment());
  }

  /**
   * Returns a color to represent the relationship.
   *
   * @param relTypeAttach which relationship attachment to get the color for
   * @return the color to represent this relationship
   */
  public static Color getRelationshipTypeAttachmentColor(
      final RelationshipTypeAttachment relTypeAttach) {
    final String archeType = relTypeAttach.getArcheType();
    switch (archeType) {
      case Constants.RELATIONSHIP_ARCHETYPE_ALLIED:
        return Color.green;
      case Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL:
        return Color.lightGray;
      case Constants.RELATIONSHIP_ARCHETYPE_WAR:
        return Color.red;
      default:
        throw new IllegalStateException(
            "Arche Type for RelationshipTypeAttachment: "
                + relTypeAttach.getName()
                + " can only be of archeType Allied, Neutral or War");
    }
  }
}
