package org.triplea.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import lombok.experimental.UtilityClass;

/**
 * container for AWT related helper functions
 */

@UtilityClass
public class Awt {
  /**
   * @param image to return a buffered image of
   * @param requiredType type of the BufferedImage to return
   * @return a BufferedImage
   */
  public BufferedImage getBufferedImage(final Image image, final int requiredType) {
    if (image instanceof BufferedImage && ((BufferedImage) image).getType() == requiredType) {
      return (BufferedImage) image;
    }

    final BufferedImage ret = new BufferedImage(
        image.getWidth(null),
        image.getHeight(null),
        requiredType);

    final Graphics2D g2d = ret.createGraphics();
    g2d.drawImage(image,0,0, null);
    g2d.dispose();

    return ret;
  }

}
