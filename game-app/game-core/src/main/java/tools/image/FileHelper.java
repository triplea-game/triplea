package tools.image;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FileHelper {

  public static Path getTextFileInRootDirectory(
      final Path mapFolderLocation, final String mapName, final String fileName) {
    Path path = null;
    if (mapFolderLocation != null && Files.exists(mapFolderLocation)) {
      path = mapFolderLocation.resolve(fileName);
    }
    if (path == null || !Files.exists(path)) {
      path = Path.of(mapName).resolveSibling(fileName);
    }
    return path;
  }
}
