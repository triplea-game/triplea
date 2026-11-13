package games.strategy.engine.framework.lookandfeel;

import games.strategy.triplea.settings.ClientSetting;
import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RelationshipColorScheme {
  STANDARD("Standard (red / green)", new Color(0xC62828), new Color(0x2E7D32), Color.LIGHT_GRAY),

  /** Orange and blue of the Okabe-Ito palette stay apart with red-green color blindness. */
  COLOR_BLIND_FRIENDLY(
      "Color blind friendly (orange / blue)",
      new Color(0xE69F00),
      new Color(0x0072B2),
      Color.LIGHT_GRAY),

  /** The colors the player picked in the settings; they have no fixed colors of their own. */
  CUSTOM("Custom", null, null, null) {
    @Override
    public Color getWar() {
      return new Color(ClientSetting.customWarColor.getValueOrThrow());
    }

    @Override
    public Color getAllied() {
      return new Color(ClientSetting.customAlliedColor.getValueOrThrow());
    }

    @Override
    public Color getNeutral() {
      return new Color(ClientSetting.customNeutralColor.getValueOrThrow());
    }
  };

  private final String displayName;
  private final Color war;
  private final Color allied;
  private final Color neutral;
}
