package games.strategy.triplea.settings.folders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.io.File;
import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.config.GameEngineProperty;
import games.strategy.engine.config.PropertyReader;

@RunWith(MockitoJUnitRunner.class)
public class FolderSettingsTest {

  @Mock
  private PropertyReader mockPropertyReader;

  @Test(expected = ClientLogger.FatalErrorException.class)
  public void folderCreateAlwaysFails() {
    Mockito.when(mockPropertyReader.readProperty(eq(GameEngineProperty.MAP_FOLDER), anyString()))
        .thenReturn("test_folder_value_would_not_exist");
    new FolderSettings(mockPropertyReader, file -> false, "defaultMapFolder", "defaultSaveFolder");
  }

  @Test
  public void happyCaseWhereFolderExists() {

    Mockito.when(mockPropertyReader.readProperty(eq(GameEngineProperty.MAP_FOLDER), anyString()))
        .thenReturn(TestData.mapFolder);
    Mockito.when(mockPropertyReader.readProperty(eq(GameEngineProperty.SAVED_GAMES_FOLDER), anyString()))
        .thenReturn(TestData.saveFolder);

    final FolderSettings settings = new FolderSettings(
        mockPropertyReader, tempFileMaker(), "default1", "default2");

    final File expectedMapFolder = new File(ClientFileSystemHelper.getUserRootFolder(), TestData.mapFolder);
    assertThat(settings.getDownloadedMapPath().getAbsolutePath(), is(expectedMapFolder.getAbsolutePath()));

    final File expectedSaveFolder = new File(ClientFileSystemHelper.getUserRootFolder(), TestData.saveFolder);
    assertThat(settings.getSaveGamePath().getAbsolutePath(), is(expectedSaveFolder.getAbsolutePath()));
  }

  private static Function<File, Boolean> tempFileMaker() {
    return file -> {
      final boolean returnValue = file.mkdirs();
      file.deleteOnExit();
      return returnValue;
    };
  }

  private interface TestData {
    String saveFolder = "testSaveGameFolderPath";
    String mapFolder = "testMapFolderPath";
  }

}
