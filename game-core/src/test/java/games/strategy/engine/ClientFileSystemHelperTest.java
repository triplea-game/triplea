package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import games.strategy.triplea.settings.GameSetting;

public final class ClientFileSystemHelperTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class GetFolderContainingFileWithNameTest {
    @Mock
    private File file;

    @Mock
    private File parentFolder;

    @Mock
    private File startFolder;

    private File getFolderContainingFileWithName() throws Exception {
      return ClientFileSystemHelper.getFolderContainingFileWithName(file.getName(), startFolder);
    }

    @BeforeEach
    public void setUp() {
      when(file.getName()).thenReturn("filename.ext");
    }

    @Test
    public void shouldReturnStartFolderWhenStartFolderContainsFile() throws Exception {
      when(file.isFile()).thenReturn(true);
      when(startFolder.listFiles()).thenReturn(new File[] {file});

      assertThat(getFolderContainingFileWithName(), is(startFolder));
    }

    @Test
    public void shouldReturnAncestorFolderWhenAncestorFolderContainsFile() throws Exception {
      when(file.isFile()).thenReturn(true);
      when(startFolder.getParentFile()).thenReturn(parentFolder);
      when(startFolder.listFiles()).thenReturn(new File[0]);
      when(parentFolder.listFiles()).thenReturn(new File[] {file});

      assertThat(getFolderContainingFileWithName(), is(parentFolder));
    }

    @Test
    public void shouldThrowExceptionWhenNoFolderContainsFile() {
      when(startFolder.getParentFile()).thenReturn(parentFolder);
      when(startFolder.listFiles()).thenReturn(new File[0]);
      when(parentFolder.listFiles()).thenReturn(new File[0]);

      assertThrows(IOException.class, () -> getFolderContainingFileWithName());
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class GetUserMapsFolderTest {
    @Mock
    private GameSetting<Path> currentSetting;
    @Mock
    private GameSetting<Path> overrideSetting;

    private Path getUserMapsFolder() {
      return ClientFileSystemHelper.getUserMapsFolder(currentSetting, overrideSetting);
    }

    @Test
    void shouldReturnCurrentFolderWhenOverrideFolderNotSet() {
      when(overrideSetting.isSet()).thenReturn(false);
      final Path currentFolder = Paths.get("/path", "to", "current");
      when(currentSetting.getValueOrThrow()).thenReturn(currentFolder);

      assertThat(getUserMapsFolder(), is(currentFolder));
    }

    @Test
    void shouldReturnOverrideFolderWhenOverrideFolderSet() {
      when(overrideSetting.isSet()).thenReturn(true);
      final Path overrideFolder = Paths.get("/path", "to", "override");
      when(overrideSetting.getValueOrThrow()).thenReturn(overrideFolder);

      assertThat(getUserMapsFolder(), is(overrideFolder));
    }
  }
}
