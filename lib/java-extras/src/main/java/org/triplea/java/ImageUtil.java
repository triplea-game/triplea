package org.triplea.java;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import lombok.experimental.UtilityClass;

/** Utility methods around {@code java.awt.Image} */
@UtilityClass
public class ImageUtil {
  public static BufferedImage convertToBufferedImage(final Image image) {
    if (image instanceof BufferedImage bufferedImage) {
      return bufferedImage;
    }
    final BufferedImage newImage =
        new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = newImage.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return newImage;
  }
}
