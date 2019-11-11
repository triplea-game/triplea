package org.triplea.test.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

@UtilityClass
public class TestDataFileReader {

  /**
   * Reads file contents, file is expected to be located at either the project root or in
   * "src/test/resources".
   *
   * @param filePath Path to the file relative to 'src/test/resources' or relative to the project
   *     root.
   * @return Contents of the file read.
   * @throws TestDataFileNotFound Thrown if file does not exist.
   */
  public static String readContents(final String filePath) {
    return readFromProjectRoot(filePath)
        // current context can be from a sub-project, or can be from the top-most level of the
        // project. Check up a directory in case context is sub-project.
        .or(() -> readFromProjectRoot("../" + filePath))
        .or(() -> readFromResources(filePath))
        .orElseThrow(() -> new TestDataFileNotFound(filePath));
  }

  private Optional<String> readFromProjectRoot(final String filePath) {
    try {
      final File f = new File(filePath);
      final List<String> lines = IOUtils.readLines(new FileInputStream(f), Charsets.UTF_8);
      return Optional.of(String.join("\n", lines));
    } catch (final IOException e) {
      return Optional.empty();
    }
  }

  @SuppressWarnings("ConstantConditions")
  private Optional<String> readFromResources(final String filePath) {
    final ClassLoader classLoader = TestDataFileReader.class.getClassLoader();
    try (InputStream inputStream = classLoader.getResourceAsStream(filePath)) {
      return Optional.of(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
    } catch (final NullPointerException | IOException e) {
      return Optional.empty();
    }
  }

  @VisibleForTesting
  static class TestDataFileNotFound extends RuntimeException {
    private static final long serialVersionUID = 6122387967083038888L;

    TestDataFileNotFound(final String filePath) {
      super("Failed to find file: " + filePath);
    }
  }
}
