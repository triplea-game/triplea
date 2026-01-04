package games.strategy.triplea;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
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
   * Loads an image from the assets folder on the classpath (defined by {@link
   * ResourceLoader#ASSETS_FOLDER}) using {@link Class#getResourceAsStream(String)}.
   *
   * @param pathComponentsRelativeToAssets segments of the path from the assets folder to an image,
   *     eg: loadImage("folder-in-assets", "image.png");
   * @return the loaded image
   */
  public BufferedImage loadImage(final String... pathComponentsRelativeToAssets) {
    String imageFilePath = createPathToImage(pathComponentsRelativeToAssets);
    try (InputStream is =
        EngineImageLoader.class.getClassLoader().getResourceAsStream(imageFilePath)) {
      if (is == null) {
        throw new IllegalStateException(
            "Error loading image at: " + createPathToImage(pathComponentsRelativeToAssets));
      } else {
        return ImageIO.read(is);
      }
    } catch (IOException e) {
      throw new IllegalStateException(
          "Error loading image at: " + imageFilePath + ", " + e.getMessage(), e);
    }
  }

  /**
   * Assembles the full path to an asset from the root of the classpath using the given path
   * components.
   *
   * <p>Note that classpath resources are always loaded using '/', regardless of the file platform
   * separator, so ensure that's the separator we're using.
   *
   * @param pathComponentsRelativeToAssets segments of the path from the assets folder to an image,
   *     eg: loadImage("folder-in-assets", "image.png");
   * @return the full path from the
   */
  private String createPathToImage(final String... pathComponentsRelativeToAssets) {
    String path =
        ResourceLoader.ASSETS_FOLDER
            + File.separator
            + String.join(File.separator, pathComponentsRelativeToAssets);
    return path.replace(File.separatorChar, '/');
  }
}
