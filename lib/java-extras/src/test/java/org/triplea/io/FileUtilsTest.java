package org.triplea.io;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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

  @Test
  @DisplayName("Verify we can read file contents with ISO-8859-1 encoded characters")
  void readContents() {
    final Path testFilePath = getPathOfResource("ISO-8859-1-test-file.txt");

    final Optional<String> contentRead = FileUtils.readContents(testFilePath);

    assertThat(contentRead, isPresent());
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
      final Path testFolderPath = getPathOfResource("test-folder-path");
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

  @Nested
  class Find {
    // Folder structure:
    // |- test-folder-path/
    //   |- test-file
    //   |- child1/
    //      |- child2/
    //         |- test-file
    final Path testFolderPath = getPathOfResource("test-folder-path");
    final Path child1 = testFolderPath.resolve("child1");
    final Path child2 = child1.resolve("child2");

    @Test
    void findClosestToRootDepth1() {
      assertThat(
          FileUtils.findClosestToRoot(child2, 1, "test-file"),
          isPresentAndIs(child2.resolve("test-file")));
    }

    @Test
    void findClosestToRootDepth3() {
      assertThat(
          FileUtils.findClosestToRoot(testFolderPath, 3, "test-file"),
          isPresentAndIs(testFolderPath.resolve("test-file")));
    }

    @Test
    void findDepth1() {
      assertThat(FileUtils.find(child2, 1, "test-file"), contains(child2.resolve("test-file")));
    }

    @Test
    void findDepth3() {
      assertThat(
          FileUtils.find(testFolderPath, 3, "test-file"),
          contains(testFolderPath.resolve("test-file"), child2.resolve("test-file")));
    }
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

  @ExtendWith(MockitoExtension.class)
  @Nested
  class ReplaceFolder {

    /**
     * Set up two temp folders, one that exists (src) with a child file and another (dest) that does
     * not exist. Do a 'replace' operation to move the 'src' folder to 'dest', verify the 'src' file
     * is deleted and then exists at the 'dest' location.
     */
    @DisplayName("If dest folder does not exist, then replace folder behaves like a move operation")
    @Test
    void destFolderDoesNotExist() throws IOException {
      final Path src = Files.createTempDirectory("temp-dir");
      Files.createFile(src.resolve("temp-file"));

      final Path dest = Files.createTempDirectory("temp-dest");
      Files.delete(dest);
      // 'dest' is guaranteed to not exist

      final boolean result = FileUtils.replaceFolder(src, dest);

      assertThat(result, is(true));
      assertThat("Destination folder should now exist", Files.exists(dest), is(true));
      assertThat(
          "'temp-file' from the source folder should now exist under the dest folder",
          Files.exists(dest.resolve("temp-file")),
          is(true));
      assertThat(
          "The original source folder is moved and should no longer exist",
          Files.exists(src),
          is(false));
    }

    @DisplayName("Verify happy case of replace folder where dest folder is cleanly overwitten")
    @Test
    void destFolderDoesExist() throws IOException {
      final Path src = Files.createTempDirectory("temp-dir");
      Files.createFile(src.resolve("temp-file"));

      final Path dest = Files.createTempDirectory("temp-dest");
      // 'dest-temp-file' is a child of 'dest' folder and should not exist after the file move
      // operation
      Files.createFile(dest.resolve("dest-temp-file"));

      final boolean result = FileUtils.replaceFolder(src, dest);

      assertThat(result, is(true));
      assertThat("Destination folder should exist", Files.exists(dest), is(true));
      assertThat(
          "Destination folder should contain the child file of 'src'",
          Files.exists(dest.resolve("temp-file")),
          is(true));
      assertThat(
          "Previous contents of destination folder should be deleted",
          Files.exists(dest.resolve("dest-temp-file")),
          is(false));
      assertThat("src folder should be moved and no longer exist", Files.exists(src), is(false));
    }

    @DisplayName("Simulate a failure to replace the dest directory, we should see a rollback")
    @Test
    void destFolderDoesExistAndFileMoveFails() throws IOException {
      final Path src = Files.createTempDirectory("temp-dir");
      Files.createFile(src.resolve("temp-file"));

      final Path dest = Files.createTempDirectory("temp-dest");
      // 'dest-temp-file' is a child of 'dest' folder and should exist after the failed file move
      Files.createFile(dest.resolve("dest-temp-file"));

      final FileUtils.FileMoveOperation fileMoveSpy =
          Mockito.spy(new FileUtils.FileMoveOperation());

      // first move is to create backup
      doCallRealMethod()
          // second move replace dest with src (simulate failure here)
          .doThrow(new IOException("simulated file move exception"))
          // third move is a rollback moving backup back to dest
          .doCallRealMethod()
          .when(fileMoveSpy)
          .move(any(), any());

      final boolean result = FileUtils.replaceFolder(src, dest, fileMoveSpy);
      assertThat(result, is(false));

      assertThat("Destination folder should exist", Files.exists(dest), is(true));
      assertThat(
          "Destination folder should contain original child file",
          Files.exists(dest.resolve("dest-temp-file")),
          is(true));
      assertThat(
          "Destination folder should not contain src child file",
          Files.exists(dest.resolve("temp-file")),
          is(false));
      assertThat(
          "Previous src folder child file should still exist",
          Files.exists(src.resolve("temp-file")),
          is(true));
    }
  }

  @Test
  void getLastModifiedForFileThatDoesNotExist() {
    final Path doesNotExist = Path.of("DNE");

    assertThat(FileUtils.getLastModified(doesNotExist), isEmpty());
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  void getLastModified() throws IOException {
    final Path tempFile = Files.createTempFile("test", "txt");

    assertThat(FileUtils.getLastModified(tempFile), isPresent());
    assertThat(FileUtils.getLastModified(tempFile).get().isBefore(Instant.now()), is(true));
  }

  private Path getPathOfResource(String name) {
    try {
      return Path.of(FileUtilsTest.class.getClassLoader().getResource("test-folder-path").toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
