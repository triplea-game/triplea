package games.strategy.engine.framework.lookandfeel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.formdev.flatlaf.FlatDarkLaf;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.RelationshipTypeAttachment;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class LookAndFeelTest extends AbstractClientSettingTestCase {
  // WCAG 2 level AA minimum for normal text
  private static final double MIN_CONTRAST_RATIO = 4.5;

  @Test
  void testIsColorDark() {
    assertThat(LookAndFeel.isColorDark(Color.BLACK)).isTrue();
    assertThat(LookAndFeel.isColorDark(Color.DARK_GRAY)).isTrue();
    assertThat(LookAndFeel.isColorDark(Color.WHITE)).isFalse();
    assertThat(LookAndFeel.isColorDark(Color.YELLOW)).isFalse();
  }

  @DisplayName("Theme colors are UI resources, so a theme change replaces them on components")
  @Test
  void themeColorsAreUiResources() {
    assertThat(LookAndFeel.getSecondaryTextColor())
        .as("updateComponentTreeUI only replaces colors that are UIResources")
        .isInstanceOf(UIResource.class);
    assertThat(LookAndFeel.getSeparatorColor()).isInstanceOf(UIResource.class);
  }

  @DisplayName("A borderless link takes no focus on a click, but Tab still reaches it")
  @Test
  void borderlessButtonTakesNoFocusOnClick() {
    final JButton link = new JButton("Open the map creator");

    LookAndFeel.setBorderlessButtonStyle(link);

    assertThat(List.of(link.isRequestFocusEnabled(), link.isFocusable()))
        .as("no focus ring after a click or on its own, keyboard players still reach it")
        .containsExactly(false, true);
  }

  @DisplayName("A primary button is filled like the FlatLaf theme's default button")
  @Test
  void primaryButtonHasDefaultButtonBackground() throws Exception {
    final String previousLookAndFeel = UIManager.getLookAndFeel().getClass().getName();
    UIManager.setLookAndFeel(new FlatDarkLaf());
    try {
      final JButton button = new JButton();

      LookAndFeel.setPrimaryButtonStyle(button);

      assertThat(centerColorOf(button))
          .as("the theme's accent fill, not the gray of the other buttons")
          .isEqualTo(UIManager.getColor("Button.default.background"));
    } finally {
      UIManager.setLookAndFeel(previousLookAndFeel);
    }
  }

  private static Color centerColorOf(final JButton button) {
    button.setSize(80, 30);
    final BufferedImage image = new BufferedImage(80, 30, BufferedImage.TYPE_INT_RGB);
    final Graphics2D graphics = image.createGraphics();
    button.paint(graphics);
    graphics.dispose();
    return new Color(image.getRGB(40, 15));
  }

  @ParameterizedTest
  @EnumSource(RelationshipColorScheme.class)
  @DisplayName("Relationship colors come from the color scheme client setting")
  void relationshipColorsFollowTheSetting(final RelationshipColorScheme scheme) {
    ClientSetting.relationshipColorScheme.setValue(scheme);

    assertThat(colorOf(Constants.RELATIONSHIP_ARCHETYPE_WAR)).isEqualTo(scheme.getWar());
    assertThat(colorOf(Constants.RELATIONSHIP_ARCHETYPE_ALLIED)).isEqualTo(scheme.getAllied());
    assertThat(colorOf(Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL)).isEqualTo(scheme.getNeutral());
  }

  private static Color colorOf(final String archeType) {
    final RelationshipTypeAttachment attachment = mock(RelationshipTypeAttachment.class);
    when(attachment.getArcheType()).thenReturn(archeType);
    return LookAndFeel.getRelationshipTypeAttachmentColor(attachment);
  }

  @ParameterizedTest
  @EnumSource(RelationshipColorScheme.class)
  @DisplayName("Relationship text meets the WCAG AA contrast ratio in every color scheme")
  void relationshipTextIsReadable(final RelationshipColorScheme scheme) {
    final List<Color> backgrounds =
        List.of(scheme.getWar(), scheme.getAllied(), scheme.getNeutral());

    for (final Color background : backgrounds) {
      final Color text = LookAndFeel.getContrastingTextColor(background);

      assertThat(contrastRatio(background, text))
          .as("text %s on background %s", text, background)
          .isGreaterThanOrEqualTo(MIN_CONTRAST_RATIO);
    }
  }

  private static double contrastRatio(final Color first, final Color second) {
    final double lighter = Math.max(relativeLuminance(first), relativeLuminance(second));
    final double darker = Math.min(relativeLuminance(first), relativeLuminance(second));
    return (lighter + 0.05) / (darker + 0.05);
  }

  // https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
  private static double relativeLuminance(final Color color) {
    return 0.2126 * linear(color.getRed())
        + 0.7152 * linear(color.getGreen())
        + 0.0722 * linear(color.getBlue());
  }

  private static double linear(final int channel) {
    final double value = channel / 255.0;
    return value <= 0.040_45 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
  }
}
