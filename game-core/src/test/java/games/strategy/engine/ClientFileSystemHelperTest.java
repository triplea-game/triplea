package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

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
  public final class GetUserMapsFolderPathTest {
    @Mock
    private GameSetting<String> currentSetting;

    @Mock
    private GameSetting<String> overrideSetting;

    private String getUserMapsFolderPath() {
      return ClientFileSystemHelper.getUserMapsFolderPath(currentSetting, overrideSetting);
    }

    @Test
    public void shouldReturnCurrentPathWhenOverridePathNotSet() {
      when(overrideSetting.isSet()).thenReturn(false);
      final String currentPath = "/path/to/current";
      when(currentSetting.getValueOrThrow()).thenReturn(currentPath);

      assertThat(getUserMapsFolderPath(), is(currentPath));
    }

    @Test
    public void shouldReturnOverridePathWhenOverridePathSet() {
      when(overrideSetting.isSet()).thenReturn(true);
      final String overridePath = "/path/to/override";
      when(overrideSetting.getValueOrThrow()).thenReturn(overridePath);

      assertThat(getUserMapsFolderPath(), is(overridePath));
    }
  }
}
