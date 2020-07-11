package games.strategy.triplea.image;

import static com.google.common.base.Preconditions.checkArgument;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class to load images from filesystem. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImageLoader {
  /**
   * Reads the given file as an image. Path is relative to the 'project root'.
   *
   * @param path Location of the file to be read as an image.
   */
  public static BufferedImage getImage(final File path) {
    checkArgument(path.exists(), "File must exist at path: " + path.getAbsolutePath());
    checkArgument(path.isFile(), "Must be a file: " + path.getAbsolutePath());
    try {
      return ImageIO.read(path);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to load image at path: " + path.getAbsolutePath(), e);
    }
  }

  public static BufferedImage getImage(final URL url) {
    try {
      return ImageIO.read(url);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to load image at path: " + url, e);
    }
  }
}
