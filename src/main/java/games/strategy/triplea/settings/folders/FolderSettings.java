package games.strategy.triplea.settings.folders;

import java.io.File;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.config.GameEngineProperty;
import games.strategy.engine.config.PropertyReader;

// TODO: move this, perhaps create a sub-package under 'settings' for additional settings that are obtained
// from config instead of the UI. Current problem is that everyting in 'triplea.settings' is related to the
// settings UI window. One option may be to group the other files under a  'settings.window' package.
public class FolderSettings {

  private static final String DEFAULT_MAP_FOLDER = "downloadedMaps";
  private static final String DEFAULT_SAVE_FOLDER = "savedGames";


  private final File downloadedMapsFolder;
  private final File saveGameFolder;

  public FolderSettings(final PropertyReader propertyFileReader) {
    this(propertyFileReader, File::mkdirs);
  }

  FolderSettings(final PropertyReader propertyReader, final Function<File, Boolean> fileMaker) {
    downloadedMapsFolder =
        ensureFolderExistsOrDie(propertyReader, GameEngineProperty.MAP_FOLDER, DEFAULT_MAP_FOLDER, fileMaker);
    saveGameFolder =
        ensureFolderExistsOrDie(propertyReader, GameEngineProperty.SAVED_GAMES_FOLDER, DEFAULT_SAVE_FOLDER, fileMaker);
  }

  private static File ensureFolderExistsOrDie(
      final PropertyReader propertyReader,
      final GameEngineProperty target,
      final String defaultValue,
      final Function<File, Boolean> fileMaker) {

    final String folderPath = propertyReader.readProperty(target, defaultValue);
    File newFolder = createFolderOrUseDefault(folderPath, DEFAULT_MAP_FOLDER);

    if (newFolder.exists()) {
      Preconditions.checkState(newFolder.isDirectory(), newFolder.getAbsolutePath() + " should be a directory");
      return newFolder;
    }

    if (!fileMaker.apply(newFolder)) {
      // failed to create, try the default
      newFolder = new File(ClientFileSystemHelper.getUserRootFolder(), DEFAULT_MAP_FOLDER);
      ClientLogger.logError(
          "Failed to create folder: " + newFolder.getAbsolutePath()
              + ", falling back to default: " + newFolder.getAbsolutePath());

      if (newFolder.exists()) {
        Preconditions.checkState(newFolder.isDirectory(), newFolder.getAbsolutePath() + " should be a directory");
        return newFolder;
      }

      if (!fileMaker.apply(newFolder)) {
        throw ClientLogger.fatal("Error, could not create folder: " + newFolder.getAbsolutePath());
      }
    }

    Preconditions.checkState(newFolder.exists(), newFolder.getAbsolutePath() + " should exist");
    Preconditions.checkState(newFolder.isDirectory(), newFolder.getAbsolutePath() + " should be a directory");
    return newFolder;
  }

  private static File createFolderOrUseDefault(final String propValue, final String defaultPath) {
    if (Strings.nullToEmpty(propValue).isEmpty()) {
      return new File(ClientFileSystemHelper.getUserRootFolder(), defaultPath);
    } else {
      return new File(ClientFileSystemHelper.getUserRootFolder(), propValue);
    }
  }




  public File getDownloadedMapPath() {
    return downloadedMapsFolder;
  }

  public File getSaveGamePath() {
    return saveGameFolder;
  }
}
