package org.triplea.io;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ZipExtractorTest {

  /**
   * Unzips a sample test zip that contains the following files and file contents:
   *
   * <pre>
   *   ./directory
   *   ./directory/test-file2 : lupsom osculus
   *   ./test-file1 : ipsum ispor
   * </pre>
   *
   * Once the zip file is unzipped, the test verifies the files exist with expected contents.
   */
  @Test
  void unzip() throws IOException, URISyntaxException {
    final Path destinationFolder = Path.of("destination");
    destinationFolder.toFile().deleteOnExit();
    try {
      final Path testDataZip = testDataZip();

      ZipExtractor.unzipFile(testDataZip, destinationFolder);

      assertThat(
          "Should contain one folder and one file only",
          FileUtils.listFiles(destinationFolder),
          hasSize(2));

      final Path unzippedTestFile1 = destinationFolder.resolve("test-file1");
      final String testFile1Contents = Files.readString(unzippedTestFile1);
      assertThat(testFile1Contents, is("ipsum ipsor\n"));

      final Path unzippedDirectory = destinationFolder.resolve("directory");
      assertThat(Files.isDirectory(unzippedDirectory), is(true));
      assertThat(Files.exists(unzippedDirectory), is(true));

      final Path unzippedTestFile2 = unzippedDirectory.resolve("test-file2");
      final String testFile2Contents = Files.readString(unzippedTestFile2);
      assertThat(testFile2Contents, is("lupsom osculus\n"));
    } finally {
      deleteDirectory(destinationFolder.toFile());
    }
    assertThat(
        "Ensure that we have no file handles open or anything else that"
            + "would get in the way of deleting the unzipped folder",
        Files.exists(destinationFolder),
        is(false));
  }

  private static Path testDataZip() throws URISyntaxException {
    final Path file =
        Path.of(ZipExtractorTest.class.getClassLoader().getResource("test-data").toURI());
    return file.resolve("test-data.zip");
  }
}
