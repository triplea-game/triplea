package org.triplea.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.awt.Image;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ImageLoader {

  /**
   * Reads the given file as an image. Path is relative to the 'project root'.
   *
   * @param path Location of the file to be read as an image.
   */
  public static Image getImage(final Path path) {
    checkArgument(
        Files.exists(path),
        "File must exist at path: "
            + path.toAbsolutePath()
            + "You should build with the checked in launcher, or and run 'downloadAssets'.");
    checkArgument(!Files.isDirectory(path), "Must be a file: " + path.toAbsolutePath());
    try {
      return ImageIO.read(path.toFile());
    } catch (final IOException e) {
      throw new RuntimeException("Unable to load image at path: " + path.toAbsolutePath(), e);
    }
  }
}
