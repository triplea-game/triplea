package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("InnerClassMayBeStatic")
final class ClientFileSystemHelperTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class GetFolderContainingFileWithNameTest {
    @Mock private File file;

    @Mock private File parentFolder;

    @Mock private File startFolder;

    private File getFolderContainingFileWithName() throws Exception {
      return ClientFileSystemHelper.getFolderContainingFileWithName(file.getName(), startFolder);
    }

    @BeforeEach
    void setUp() {
      when(file.getName()).thenReturn("filename.ext");
    }

    @Test
    void shouldReturnStartFolderWhenStartFolderContainsFile() throws Exception {
      when(file.isFile()).thenReturn(true);
      when(startFolder.listFiles()).thenReturn(new File[] {file});

      assertThat(getFolderContainingFileWithName(), is(startFolder));
    }

    @Test
    void shouldReturnAncestorFolderWhenAncestorFolderContainsFile() throws Exception {
      when(file.isFile()).thenReturn(true);
      when(startFolder.getParentFile()).thenReturn(parentFolder);
      when(startFolder.listFiles()).thenReturn(new File[0]);
      when(parentFolder.listFiles()).thenReturn(new File[] {file});

      assertThat(getFolderContainingFileWithName(), is(parentFolder));
    }

    @Test
    void shouldThrowExceptionWhenNoFolderContainsFile() {
      when(startFolder.getParentFile()).thenReturn(parentFolder);
      when(startFolder.listFiles()).thenReturn(new File[0]);
      when(parentFolder.listFiles()).thenReturn(new File[0]);

      assertThrows(IOException.class, this::getFolderContainingFileWithName);
    }
  }

  @Nested
  final class GetUserMapsFolderTest extends AbstractClientSettingTestCase {
    @Test
    void shouldReturnCurrentFolderWhenOverrideFolderNotSet() {
      final File result =
          ClientFileSystemHelper.getUserMapsFolder(() -> new File("/path/to/current"));

      assertThat(result, is(Paths.get("/path", "to", "current", "downloadedMaps").toFile()));
    }

    @Test
    void shouldReturnOverrideFolderWhenOverrideFolderSet() {
      ClientSetting.mapFolderOverride.setValue(Paths.get("/path", "to", "override"));

      final File result =
          ClientFileSystemHelper.getUserMapsFolder(() -> new File("/path/to/current"));

      assertThat(result, is(Paths.get("/path", "to", "override").toFile()));
    }
  }
}
