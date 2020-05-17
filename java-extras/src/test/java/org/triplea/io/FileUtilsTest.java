package org.triplea.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.io.File;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class ListFilesTest {
    @Mock private File directory;

    @Test
    void shouldReturnFileCollectionWhenTargetIsDirectory() {
      final File file1 = new File("file1");
      final File file2 = new File("file2");
      final File file3 = new File("file3");
      when(directory.listFiles()).thenReturn(new File[] {file1, file2, file3});

      assertThat(FileUtils.listFiles(directory), contains(file1, file2, file3));
    }

    @Test
    void shouldReturnEmptyCollectionWhenTargetIsNotDirectory() {
      when(directory.listFiles()).thenReturn(null);

      assertThat(FileUtils.listFiles(directory), is(empty()));
    }
  }
}
