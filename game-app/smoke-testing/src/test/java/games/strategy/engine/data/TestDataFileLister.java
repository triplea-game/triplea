package games.strategy.engine.data;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

@UtilityClass
class TestDataFileLister {

  /**
   * Returns a list of files located in folder located at the root of the classpath.
   *
   * @param folderName Name of the folder on the classpath
   * @return List of all files located in specified folder
   */
  Collection<Path> listFilesInTestClasspathDir(final @NonNls String folderName) throws IOException {
    URL url = TestDataFileLister.class.getClassLoader().getResource(folderName);
    if (url == null) {
      throw new IllegalStateException("Folder " + folderName + " not found on classpath");
    }

    File folder;
    try {
        folder = new File(url.toURI());
    } catch (URISyntaxException e) {
        throw new IOException(e);
    }
    File[] files = folder.listFiles();

    List<Path> results = new ArrayList<>();
    if (files != null) {
      for (File f : files) {
        results.add(f.toPath());
      }
    }

    return results.stream()
        .sorted(Comparator.comparing(Path::getFileName, Comparator.comparing(Path::toString)))
        .collect(Collectors.toList());
  }
}
