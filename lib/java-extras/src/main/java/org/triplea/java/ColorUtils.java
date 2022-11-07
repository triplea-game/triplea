package org.triplea.java;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.util.Random;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ColorUtils {
  /**
   * Returns a color parsed from the provided input hex string.
   *
   * @param colorString EG: 00FF00, FF00FF, 000000
   */
  public Color fromHexString(final String colorString) {
    Preconditions.checkArgument(
        colorString.length() == 6,
        "Colors must be 6 digit hex numbers, eg FF0011, not: " + colorString);
    try {
      return new Color(Integer.decode("0x" + colorString));
    } catch (final NumberFormatException nfe) {
      throw new IllegalArgumentException(
          "Colors must be 6 digit hex numbers, eg FF0011, not: "
              + colorString
              + ", "
              + nfe.getMessage(),
          nfe);
    }
  }

  /** Returns a randomly generated color using a fixed random seed. */
  public Color randomColor(final long randomSeed) {
    final Random random = new Random(randomSeed);
    return Color.getHSBColor(random.nextFloat(), random.nextFloat(), random.nextFloat());
  }
}
