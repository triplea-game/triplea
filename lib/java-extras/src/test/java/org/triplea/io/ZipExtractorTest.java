package org.triplea.io;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
  void unzip() throws IOException {
    final File destinationFolder = new File("destination");
    destinationFolder.deleteOnExit();
    try {
      final File testDataZip = testDataZip();

      ZipExtractor.unzipFile(testDataZip, destinationFolder);

      assertThat(
          "Should contain one folder and one file only",
          FileUtils.listFiles(destinationFolder.toPath()),
          hasSize(2));

      final File unzippedTestFile1 = new File(destinationFolder, "test-file1");
      final String testFile1Contents = Files.readString(unzippedTestFile1.toPath());
      assertThat(testFile1Contents, is("ipsum ipsor\n"));

      final File unzippedDirectory = new File(destinationFolder, "directory");
      assertThat(unzippedDirectory.isDirectory(), is(true));
      assertThat(unzippedDirectory.exists(), is(true));

      final File unzippedTestFile2 = new File(unzippedDirectory, "test-file2");
      final String testFile2Contents = Files.readString(unzippedTestFile2.toPath());
      assertThat(testFile2Contents, is("lupsom osculus\n"));
    } finally {
      deleteDirectory(destinationFolder);
    }
    assertThat(
        "Ensure that we have no file handles open or anything else that"
            + "would get in the way of deleting the unzipped folder",
        destinationFolder.exists(),
        is(false));
  }

  private static File testDataZip() {
    final File file =
        new File(ZipExtractorTest.class.getClassLoader().getResource("test-data").getFile());
    return new File(file, "test-data.zip");
  }
}
