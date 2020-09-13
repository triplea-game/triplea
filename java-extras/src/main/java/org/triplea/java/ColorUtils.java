package org.triplea.java;

import com.google.common.base.Preconditions;
import java.awt.Color;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ColorUtils {
  public Color fromHexString(final String colorString) {
    Preconditions.checkArgument(
        colorString.length() == 6,
        "Colors must be 6 digit hex numbers, eg FF0011, not: " + colorString);
    try {
      return new Color(Integer.decode("0x" + colorString));
    } catch (final NumberFormatException nfe) {
      throw new IllegalArgumentException(
          "Colors must be 6 digit hex numbers, eg FF0011, not: " + colorString);
    }
  }
}
