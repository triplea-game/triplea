package games.strategy.triplea.settings.folders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.io.File;

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
    new FolderSettings(mockPropertyReader, file -> false);
  }

  @Test
  public void happyCaseWhereFolderExists() {

    Mockito.when(mockPropertyReader.readProperty(eq(GameEngineProperty.MAP_FOLDER), anyString()))
        .thenReturn(TestData.mapFolder);
    Mockito.when(mockPropertyReader.readProperty(eq(GameEngineProperty.SAVED_GAMES_FOLDER), anyString()))
        .thenReturn(TestData.saveFolder);


    final FolderSettings settings = new FolderSettings(mockPropertyReader); //, File::mkdirfile -> true);

    final File expectedMapFolder = new File(ClientFileSystemHelper.getUserRootFolder(), TestData.mapFolder);
    assertThat(settings.getDownloadedMapPath().getAbsolutePath(), is(expectedMapFolder.getAbsolutePath()));

    final File expectedSaveFolder = new File(ClientFileSystemHelper.getUserRootFolder(), TestData.saveFolder);
    assertThat(settings.getSaveGamePath().getAbsolutePath(), is(expectedSaveFolder.getAbsolutePath()));
  }

  private interface TestData {
    String saveFolder = "testSaveGameFolderPath";
    String mapFolder = "testMapFolderPath";
  }

}
