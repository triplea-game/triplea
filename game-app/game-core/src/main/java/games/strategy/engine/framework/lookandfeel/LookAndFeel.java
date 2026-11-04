package games.strategy.engine.framework.lookandfeel;

import static com.google.common.base.Preconditions.checkState;

import com.formdev.flatlaf.FlatClientProperties;
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
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
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
"""
Look and feel changes can cause instability.
Please restart all running TripleA instances.""",
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
    lookAndFeels.addAll(List.of(UIManager.getInstalledLookAndFeels()));
    lookAndFeels.addAll(getSubstanceLookAndFeels());
    return lookAndFeels;
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

  public static Color getContrastingTextColor(final Color background) {
    return isColorDark(background) ? Color.WHITE : Color.BLACK;
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

    if (!SystemProperties.isMac() && !SystemProperties.isWindows()) {
      // FlatLaf paints popup drop shadows by wrapping popups in heavyweight translucent windows.
      // Under X11/Wayland those windows are unreliable, leaving combo box and menu popups
      // invisible.
      // Disabling the painted shadow on Linux falls back to standard lightweight Swing popups.
      UIManager.put("Popup.dropShadowPainted", Boolean.FALSE);
    }
  }

  /**
   * Marks the button as the main action of its screen, eg "Update": FlatLaf themes paint it in the
   * colors of their default button; other look and feels ignore it.
   */
  public static void setPrimaryButtonStyle(final AbstractButton button) {
    button.putClientProperty(
        FlatClientProperties.STYLE,
        "background: $Button.default.background;"
            + " foreground: $Button.default.foreground;"
            + " borderColor: $Button.default.borderColor");
  }

  /**
   * Shows the button without a frame until the mouse is over it, for secondary actions like links
   * to tools; FlatLaf themes only. A click does not move the focus to it, so it shows no focus ring
   * unless the player reaches it with the keyboard (Tab still does).
   */
  public static void setBorderlessButtonStyle(final AbstractButton button) {
    button.putClientProperty(
        FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
    button.setRequestFocusEnabled(false);
  }

  /** Shows the component in the smaller font of the theme, eg for compact buttons; FlatLaf only. */
  public static void setSmallStyle(final JComponent component) {
    component.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
  }

  /**
   * Returns the theme's color for secondary text, eg a hint below a title. It is a {@link
   * ColorUIResource}: after a theme change the text takes the new theme's normal text color instead
   * of keeping the old theme's gray, until the restart a theme change asks for.
   */
  public static Color getSecondaryTextColor() {
    return getThemeColor("Label.disabledForeground");
  }

  /** Returns the theme's accent color, eg of focus rings and selections. */
  public static Color getAccentColor() {
    return getThemeColor("Component.accentColor");
  }

  /** Returns the theme's color for thin lines that separate areas of a window. */
  public static Color getSeparatorColor() {
    return getThemeColor("Separator.foreground");
  }

  /** Creates the border of a window's footer: a separator line on top, room around its buttons. */
  public static Border newFooterBorder() {
    return BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, getSeparatorColor()),
        BorderFactory.createEmptyBorder(10, 12, 10, 12));
  }

  private static Color getThemeColor(final @NonNls String key) {
    return new ColorUIResource(Optional.ofNullable(UIManager.getColor(key)).orElse(Color.GRAY));
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
    final RelationshipColorScheme colors = ClientSetting.relationshipColorScheme.getValueOrThrow();
    final String archeType = relTypeAttach.getArcheType();
    switch (archeType) {
      case Constants.RELATIONSHIP_ARCHETYPE_ALLIED:
        return colors.getAllied();
      case Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL:
        return colors.getNeutral();
      case Constants.RELATIONSHIP_ARCHETYPE_WAR:
        return colors.getWar();
      default:
        throw new IllegalStateException(
            "Arche Type for RelationshipTypeAttachment: "
                + relTypeAttach.getName()
                + " can only be of archeType Allied, Neutral or War");
    }
  }
}
