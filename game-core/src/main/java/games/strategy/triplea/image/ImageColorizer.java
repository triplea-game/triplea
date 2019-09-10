package games.strategy.triplea.image;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Class used to colorize images. Based on finding the luminance from the original image then
 * applying hue, saturation, and brightness to colorize it.
 */
public class ImageColorizer {

  private static final int MAX_COLOR = 256;
  private static final float LUMINANCE_RED = 0.2126f;
  private static final float LUMINANCE_GREEN = 0.7152f;
  private static final float LUMINANCE_BLUE = 0.0722f;

  /**
   * Apply color to the given image. This takes the hue and saturation of the given color and
   * applies it to the image. It ignores the brightness of the color which can cause images to
   * become poor quality.
   */
  public static void applyColor(final Color color, final BufferedImage image) {
    final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    applyHsb((int) (hsb[0] * 360), (int) (hsb[1] * 100), 0, image);
  }

  /**
   * Apply HSB values to the given image.
   *
   * @param hue 0 to 360
   * @param saturation 0 to 100
   * @param brightness -100 to 100, which adjusts the given image's brightness
   * @param image the image to transform
   */
  public static void applyHsb(
      final int hue, final int saturation, final int brightness, final BufferedImage image) {

    // Create color lookup table for luminance values
    final int[] redLookup = new int[MAX_COLOR];
    final int[] greenLookup = new int[MAX_COLOR];
    final int[] blueLookup = new int[MAX_COLOR];
    final float hueFloat = hue / 360f;
    final float saturationFloat = saturation / 100f;
    for (int i = 0; i < MAX_COLOR; i++) {
      final float brightnessFloat = i / 255f;
      final Color color = Color.getHSBColor(hueFloat, saturationFloat, brightnessFloat);
      redLookup[i] = (color.getRed());
      greenLookup[i] = (color.getGreen());
      blueLookup[i] = (color.getBlue());
    }

    // Loop through image to colorize each pixel
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        final Color color = new Color(image.getRGB(x, y), true);

        // Don't worry about colorization if pixel is transparent
        if (color.getAlpha() == 0) {
          continue;
        }

        // Avoid colorization if pixel is close to black so image stays sharp
        final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        if (hsb[2] < 0.1) {
          continue;
        }

        // Find luminance and use lookup table to set resulting color
        final int lum = findLuminance(color, brightness);
        final Color finalColor =
            new Color(redLookup[lum], greenLookup[lum], blueLookup[lum], color.getAlpha());
        image.setRGB(x, y, finalColor.getRGB());
      }
    }
  }

  private static int findLuminance(final Color color, final int brightness) {
    int lum =
        (int)
            (color.getRed() * LUMINANCE_RED
                + color.getGreen() * LUMINANCE_GREEN
                + color.getBlue() * LUMINANCE_BLUE);
    if (brightness > 0) {
      lum = (int) (lum * (100f - brightness) / 100f);
      lum += 255f - (100f - brightness) * 255f / 100f;
    } else if (brightness < 0) {
      lum = (int) ((lum * (brightness + 100f)) / 100f);
    }
    return lum;
  }
}
