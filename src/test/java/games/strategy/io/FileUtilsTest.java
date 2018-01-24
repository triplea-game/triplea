package games.strategy.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;

public final class FileUtilsTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class ListFilesTest {
    @Spy
    private final File directory = new File("");

    @Test
    public void shouldReturnFileCollectionWhenTargetIsDirectory() {
      final File file1 = new File("file1");
      final File file2 = new File("file2");
      final File file3 = new File("file3");
      when(directory.listFiles()).thenReturn(new File[] {file1, file2, file3});

      assertThat(FileUtils.listFiles(directory), contains(file1, file2, file3));
    }

    @Test
    public void shouldReturnEmptyCollectionWhenTargetIsNotDirectory() {
      when(directory.listFiles()).thenReturn(null);

      assertThat(FileUtils.listFiles(directory), is(empty()));
    }
  }
}
