package tools.util;

import games.strategy.ui.Util;
import java.awt.Image;
import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FileHelper {

  /**
   * Helper method to unify commons file searching functionality. It searches for a file with the
   * name of {@code fileName} inside of {@code mapFolderLocation}. If no file exists there it tries
   * to search inside a directory specified by {@code mapFolder}.
   *
   * @param mapFolderLocation The folder to check first.
   * @param mapFolder The folder to check as a fallback.
   * @param fileName The file to search for.
   * @return The found file if it exists in {@code mapFolderLocation}, the file in {@code mapFolder}
   *     otherwise, even if it does not exist.
   */
  public static Path getFileInMapRoot(
      final Path mapFolderLocation, final Path mapFolder, final String fileName) {
    if (mapFolderLocation != null && Files.exists(mapFolderLocation)) {
      final Path path = mapFolderLocation.resolve(fileName);
      if (Files.exists(path)) {
        return path;
      }
    }
    return mapFolder.resolveSibling(fileName);
  }

  /**
   * Creates the image map and makes sure it is properly loaded.
   *
   * @param mapFolder the path of image map
   */
  public static Image newImage(final Path mapFolder) {
    final Image image = Toolkit.getDefaultToolkit().createImage(mapFolder.toString());
    Util.ensureImageLoaded(image);
    return image;
  }
}
