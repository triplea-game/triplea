package org.triplea.io;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZipExtractorTest {

  private Path destinationFolder;
  private Path testDataFolder;

  @BeforeEach
  void setUp() throws Exception {
    destinationFolder = Files.createTempDirectory("zip-destination");
    testDataFolder =
        Path.of(ZipExtractorTest.class.getClassLoader().getResource("test-data").toURI());
  }

  @AfterEach
  void tearDown() throws IOException {
    deleteDirectory(destinationFolder.toFile());
    assertThat(
        "Ensure that we have no file handles open or anything else that"
            + "would get in the way of deleting the unzipped folder",
        Files.exists(destinationFolder),
        is(false));
  }

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
  void unzip() throws IOException {
    final Path testDataZip = testDataFolder.resolve("test-data.zip");

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
  }

  @Test
  void verifyMaliciousZipCantBeUnpacked() {
    final Path zip = testDataFolder.resolve("evil.zip");
    final Path subfolder = destinationFolder.resolve("sub");

    final Exception exception =
        assertThrows(
            ZipExtractor.ZipReadException.class, () -> ZipExtractor.unzipFile(zip, subfolder));

    assertThat(Files.exists(destinationFolder.resolve("matrix.jpg")), is(false));
    // Make sure file isn't extracted at all
    assertThat(Files.exists(subfolder.resolve("matrix.jpg")), is(false));

    // Folder creation outside of the extraction directory should be prevented!
    assertThat(exception.getMessage(), containsString(".."));
  }
}
