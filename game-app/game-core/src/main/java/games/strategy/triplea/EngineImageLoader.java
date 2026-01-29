package games.strategy.triplea;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import lombok.experimental.UtilityClass;

/**
 * Utility class that can load image assets.
 *
 * <p>This class can *ONLY* load images available as part of the game jar in the {@code assets}
 * directory, not custom images available as part of downloaded game maps. Do *not* use this to read
 * images from maps.
 */
@UtilityClass
public class EngineImageLoader {
  public Image loadFrameIcon() {
    return loadImage("icons", "ta_icon.png");
  }

  /**
   * Loads an image from the assets folder on the classpath using {@link
   * Class#getResourceAsStream(String)}.
   *
   * @param assetsImageFileStrings segments of the path from the assets folder to an image, eg:
   *     loadImage("folder-in-assets", "image.png");
   * @return the loaded image
   */
  public BufferedImage loadImage(final String... assetsImageFileStrings) {
    String imageFileLocation = ResourceLoader.getAssetsFileLocation(assetsImageFileStrings);
    try (InputStream is =
        EngineImageLoader.class.getClassLoader().getResourceAsStream(imageFileLocation)) {
      if (is == null) {
        throw new IllegalStateException(
            "Error loading image at: "
                + imageFileLocation
                + ", input stream is null (check that the resource exists on the classpath at this location)");
      } else {
        return ImageIO.read(is);
      }
    } catch (IOException e) {
      throw new IllegalStateException(
          "Error loading image at: " + imageFileLocation + ", " + e.getMessage(), e);
    }
  }
}
