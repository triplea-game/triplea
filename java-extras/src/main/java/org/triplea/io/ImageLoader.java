package org.triplea.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ImageLoader {

  /**
   * Reads the given file as an image. Path is relative to the 'project root'.
   *
   * @param path Location of the file to be read as an image.
   */
  public static Image getImage(final File path) {
    checkArgument(path.exists(), "File must exist at path: " + path.getAbsolutePath());
    checkArgument(path.isFile(), "Must be a file: " + path.getAbsolutePath());
    try {
      return ImageIO.read(path);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to load image at path: " + path.getAbsolutePath(), e);
    }
  }
}
