package games.strategy.engine.framework.lookandfeel;

import java.awt.Color;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter(AccessLevel.PACKAGE)
public enum RelationshipColorScheme {
  STANDARD("Standard (red / green)", new Color(0xC62828), new Color(0x2E7D32), Color.LIGHT_GRAY),

  /** Orange and blue of the Okabe-Ito palette stay apart with red-green color blindness. */
  COLOR_BLIND_FRIENDLY(
      "Color blind friendly (orange / blue)",
      new Color(0xE69F00),
      new Color(0x0072B2),
      Color.LIGHT_GRAY);

  @Getter private final String displayName;
  private final Color war;
  private final Color allied;
  private final Color neutral;
}
