package games.strategy.engine.data;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
class TestDataFileLister {

  /**
   * Returns a list of files located in folder like: 'test/resources/{folder-name}'.
   *
   * @param folderName Name of the folder in 'test/resources'
   * @return List of all files located in specified folder.
   */
  Collection<File> listFilesInTestResourcesDirectory(final String folderName) {
    return Arrays.stream(findFilesInFolder(folderName).listFiles())
        .sorted(Comparator.comparing(File::getName))
        .collect(Collectors.toList());
  }

  private static File findFilesInFolder(final String folderName) {
    final File mapXmls = Paths.get("src", "test", "resources", folderName).toFile();
    if (mapXmls.exists()) {
      return mapXmls;
    }

    return Paths.get("smoke-testing", "src", "test", "resources", folderName).toFile();
  }
}
