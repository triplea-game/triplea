package org.triplea.io;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
final class FileUtilsTest {
  @Nested
  final class ListFilesTest {
    @Test
    void shouldReturnFileCollectionWhenTargetIsDirectory() throws Exception {
      final Path tempDir = Files.createTempDirectory(null);
      Files.createFile(tempDir.resolve("file1"));
      Files.createFile(tempDir.resolve("file2"));
      Files.createFile(tempDir.resolve("file3"));

      assertThat(
          FileUtils.listFiles(tempDir),
          containsInAnyOrder(
              tempDir.resolve("file1"), tempDir.resolve("file2"), tempDir.resolve("file3")));
    }

    @Test
    void shouldReturnEmptyCollectionWhenTargetIsNotDirectory() throws Exception {
      final Path tempDir = Files.createTempDirectory(null);
      final Path file1 = Files.createFile(tempDir.resolve("file1"));

      assertThat(FileUtils.listFiles(file1), is(empty()));
    }

    @Test
    void shouldReturnEmptyCollectionWhenDirectoryIsEmpty() throws Exception {
      final Path tempDir = Files.createTempDirectory(null);

      assertThat(FileUtils.listFiles(tempDir), is(empty()));
    }
  }

  @Nested
  class FindFileInParentFolder {

    @Test
    void fileDoesNotExist() {
      final Optional<Path> result =
          FileUtils.findFileInParentFolders(Path.of(""), "does not exist");
      assertThat(result, isEmpty());
    }

    @Test
    void fileExists() {
      // Folder structure:
      // |- test-folder-path/
      //   |- touch-parent
      //   |- child1/
      //      |- child2/
      //         |- touch-file

      final Path testFolderPath =
          Path.of(
              ZipExtractorTest.class.getClassLoader().getResource("test-folder-path").getFile());
      final Path child1 = testFolderPath.resolve("child1");
      final Path child2 = child1.resolve("child2");

      assertThat(
          "child2 folder contains 'touch-file'",
          FileUtils.findFileInParentFolders(child2, "touch-file"),
          isPresentAndIs(child2.resolve("touch-file")));

      assertThat(
          "child1 nor parents do not contain'touch-file'",
          FileUtils.findFileInParentFolders(child1, "touch-file"),
          isEmpty());

      assertThat(
          "all three test folder contain 'touch-parent' at a top level",
          FileUtils.findFileInParentFolders(child1, "touch-parent"),
          isPresentAndIs(testFolderPath.resolve("touch-parent")));

      assertThat(
          FileUtils.findFileInParentFolders(child2, "touch-parent"),
          isPresentAndIs(testFolderPath.resolve("touch-parent")));

      assertThat(
          FileUtils.findFileInParentFolders(testFolderPath, "touch-parent"),
          isPresentAndIs(testFolderPath.resolve("touch-parent")));
    }
  }

  @Test
  @DisplayName("Verify we can read file contents with ISO-8859-1 encoded characters")
  void readContents() {
    final Path testFile =
        Path.of(
            ZipExtractorTest.class
                .getClassLoader()
                .getResource("ISO-8859-1-test-file.txt")
                .getFile());

    final Optional<String> contentRead = FileUtils.readContents(testFile);

    assertThat(contentRead, isPresent());
  }

  /**
   * Creates a non-empty directory, run delete, verify directory is deleted. Linux systems will fail
   * the delete operation if the directory is not empty, this test verifies that we do a recursive
   * delete before removing the directory.
   */
  @Test
  void deleteDirectory() throws IOException {
    final Path directory = Path.of("directory");
    Files.createDirectory(directory);
    // create a file in the directory so that it is non-empty
    Files.createFile(directory.resolve("touch-file"));
    assertThat("Precondition, make sure the directory exists", Files.exists(directory), is(true));

    FileUtils.deleteDirectory(directory);

    assertThat(Files.exists(directory), is(false));
  }
}
