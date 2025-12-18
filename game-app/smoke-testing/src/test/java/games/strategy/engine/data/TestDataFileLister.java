package games.strategy.engine.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.Postconditions;

@UtilityClass
class TestDataFileLister {

  /**
   * Returns a list of files located in folder like: 'test/resources/{folder-name}'.
   *
   * @param folderName Name of the folder in 'test/resources'
   * @return List of all files located in specified folder.
   */
  Collection<Path> listFilesInTestResourcesDirectory(final @NonNls String folderName)
      throws IOException {
    return Files.list(findFilesInFolder(folderName))
        .sorted(Comparator.comparing(Path::getFileName, Comparator.comparing(Path::toString)))
        .collect(Collectors.toList());
  }

  private static Path findFilesInFolder(final String folderName) {
    Path resourcesFolder = Path.of("src", "test", "resources", folderName);
    if (!Files.exists(resourcesFolder)) {
      resourcesFolder = Path.of("smoke-testing", "src", "test", "resources", folderName);
    }
    if (!Files.exists(resourcesFolder)) {
      resourcesFolder =
          Path.of("game-app", "smoke-testing", "src", "test", "resources", folderName);
    }

    Postconditions.assertState(
        Files.exists(resourcesFolder), "Could not find folder: " + folderName);
    return resourcesFolder;
  }
}
