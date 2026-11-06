package games.strategy.engine.framework.lookandfeel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.RelationshipTypeAttachment;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @EnumSource(value = RelationshipColorScheme.class, mode = EXCLUDE, names = "CUSTOM")
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

  @Test
  @DisplayName("The custom color scheme uses the colors the player picked")
  void customSchemeUsesPickedColors() {
    givenCustomColors(Color.MAGENTA, Color.CYAN, Color.YELLOW);

    assertThat(colorOf(Constants.RELATIONSHIP_ARCHETYPE_WAR))
        .as("war uses the picked color")
        .isEqualTo(Color.MAGENTA);
    assertThat(colorOf(Constants.RELATIONSHIP_ARCHETYPE_ALLIED))
        .as("allied uses the picked color")
        .isEqualTo(Color.CYAN);
    assertThat(colorOf(Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL))
        .as("neutral uses the picked color")
        .isEqualTo(Color.YELLOW);
  }

  private static void givenCustomColors(final Color war, final Color allied, final Color neutral) {
    ClientSetting.relationshipColorScheme.setValue(RelationshipColorScheme.CUSTOM);
    ClientSetting.customWarColor.setValue(war.getRGB());
    ClientSetting.customAlliedColor.setValue(allied.getRGB());
    ClientSetting.customNeutralColor.setValue(neutral.getRGB());
  }

  @ParameterizedTest
  @ValueSource(strings = {"#c62828", "C62828", " #c62828 "})
  @DisplayName("A hex color is read with or without # and surrounding blanks")
  void parseHexColorReadsHexColors(final String text) {
    assertThat(LookAndFeel.parseHexColor(text)).contains(new Color(0xC62828));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "#", "#c6282", "#c628280", "#c6282g", "red"})
  @DisplayName("Anything but six hex digits is no color")
  void parseHexColorRejectsOtherText(final String text) {
    assertThat(LookAndFeel.parseHexColor(text)).isEmpty();
  }

  @Test
  @DisplayName("A color survives the round trip through hex")
  void hexRoundTrip() {
    final Color color = new Color(0x0072B2);

    assertThat(LookAndFeel.parseHexColor(LookAndFeel.convertColorToHex(color))).contains(color);
  }

  @ParameterizedTest
  @EnumSource(value = RelationshipColorScheme.class, mode = EXCLUDE, names = "CUSTOM")
  @DisplayName("Relationship text meets the WCAG AA contrast ratio in every preset color scheme")
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

  @ParameterizedTest
  @ValueSource(
      ints = {0xFF0000, 0x777777, 0x006DFB, 0x00FF00, 0x0000FF, 0xFFFF00, 0x000000, 0xFFFFFF})
  @DisplayName("Text on a custom color is the more readable of black and white")
  void contrastingTextIsTheMoreReadableOfBlackAndWhite(final int rgb) {
    final Color background = new Color(rgb);
    final Color text = LookAndFeel.getContrastingTextColor(background);
    final Color otherText = text.equals(Color.BLACK) ? Color.WHITE : Color.BLACK;

    assertThat(contrastRatio(background, text))
        .as("text %s on background %s", text, background)
        .isGreaterThanOrEqualTo(contrastRatio(background, otherText));
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
