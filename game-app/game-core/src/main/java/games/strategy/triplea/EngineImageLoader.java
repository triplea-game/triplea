package games.strategy.triplea;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;
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
    // simple function to return a resource stream if we can read it
    Function<String, Optional<InputStream>> tryStream =
        path ->
            Optional.ofNullable(EngineImageLoader.class.getClassLoader().getResourceAsStream(path));

    // first attempt to read the resource is with the assets folder in the path, this is the typical case
    final InputStream imageStream =
        tryStream
            .apply(ResourceLoader.getAssetsFileLocation(assetsImageFileStrings))
            // Dirty hack Fallback for IDES to read the resource directly.
            // This will work if the classpath to the 'assets' folder has been set explicitly in IDE,
            // eg: check IDEA headedGameRunner launcher configuration.
            .or(() -> tryStream.apply(String.join("/", assetsImageFileStrings)))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Image not found error: "
                            + assetsImageFileStrings
                            + ", check that asset folder is present."));

    try (imageStream) {
      return ImageIO.read(imageStream);
    } catch (IOException e) {
      throw new RuntimeException("Unexpected error reading image file", e);
    }
  }
}
