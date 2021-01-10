package org.triplea.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
final class FileUtilsTest {

  @Nested
  final class NewFile {
    @Test
    void createNewFile() {
      assertThat(FileUtils.newFile("file", "path"), is(new File("file" + File.separator + "path")));
    }

    @Test
    void createNewFileFromSingleton() {
      assertThat(FileUtils.newFile("file"), is(new File("file")));
    }
  }

  @Nested
  final class ListFilesTest {
    @Test
    void shouldReturnFileCollectionWhenTargetIsDirectory() throws Exception {
      final Path tempDir = Files.createTempDirectory(null);
      Files.createFile(tempDir.resolve("file1"));
      Files.createFile(tempDir.resolve("file2"));
      Files.createFile(tempDir.resolve("file3"));

      assertThat(
          FileUtils.listFiles(tempDir.toFile()),
          containsInAnyOrder(
              tempDir.resolve("file1").toFile(),
              tempDir.resolve("file2").toFile(),
              tempDir.resolve("file3").toFile()));
    }

    @Test
    void shouldReturnEmptyCollectionWhenTargetIsNotDirectory() throws Exception {
      final Path tempDir = Files.createTempDirectory(null);
      final File file1 = Files.createFile(tempDir.resolve("file1")).toFile();

      assertThat(FileUtils.listFiles(file1), is(empty()));
    }

    @Test
    void shouldReturnEmptyCollectionWhenDirectoryIsEmpty() throws Exception {
      final Path tempDir = Files.createTempDirectory(null);

      assertThat(FileUtils.listFiles(tempDir.toFile()), is(empty()));
    }
  }
}
