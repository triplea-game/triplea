package games.strategy.triplea;

import games.strategy.engine.ClientFileSystemHelper;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import lombok.experimental.UtilityClass;

/**
 * Use this to load images from the game engine 'assets' folder. Do *not* use this to read images
 * from maps.
 */
@UtilityClass
public class EngineImageLoader {
  public static final String ASSETS_FOLDER = "assets";

  public Image loadFrameIcon() {
    return loadImage("icons", Constants.FILE_NAME_IMAGE_TA_ICON);
  }

  /**
   * Reads an image from the assets folder.
   *
   * @param path Path from assets folder to image, eg: loadImage("folder-in-assets", "image.png");
   */
  public BufferedImage loadImage(final String... path) {
    Path imageFilePath = createPathToImage(path);

    if (!Files.exists(imageFilePath)) {
      imageFilePath = ClientFileSystemHelper.getRootFolder().resolve(createPathToImage(path));
    }

    if (!Files.exists(imageFilePath)) {
      throw new IllegalStateException(
          "Error loading image, image does not exist at: " + imageFilePath.toAbsolutePath());
    }

    try {
      return ImageIO.read(imageFilePath.toFile());
    } catch (final IOException e) {
      throw new IllegalStateException(
          "Error loading image at: " + imageFilePath.toAbsolutePath() + ", " + e.getMessage(), e);
    }
  }

  private Path createPathToImage(final String... path) {
    Path imageFilePath = Path.of(ASSETS_FOLDER);
    for (final String pathPart : path) {
      imageFilePath = imageFilePath.resolve(pathPart);
    }
    return imageFilePath;
  }
}
