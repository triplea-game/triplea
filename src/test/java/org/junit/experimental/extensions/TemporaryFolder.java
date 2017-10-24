package org.junit.experimental.extensions;

import java.io.File;
import java.io.IOException;

/**
 * Based off
 * https://raw.githubusercontent.com/rherrmann/junit5-experiments/master/src/main/java/com/codeaffine/junit5/TemporaryFolder.java
 * Replacement for the JUnit 4 TemporaryFolder.
 */
public class TemporaryFolder {
  private File rootFolder;

  /**
   * Creates and returns a new temporary file which gets deleted when the Virtual Machine Terminates.
   */
  public File newFile(final String name) throws IOException {
    final File result = new File(rootFolder, name);
    result.createNewFile();
    result.deleteOnExit();
    return result;
  }

  void prepare() {
    try {
      rootFolder = File.createTempFile("junit5-", ".tmp");
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    }
    rootFolder.delete();
    rootFolder.mkdirs();
    rootFolder.deleteOnExit();
  }
}
