package games.strategy.triplea.settings.folders;

import java.io.File;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.SystemPreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class FolderSettings implements HasDefaults {

  private final static File DEFAULT_DOWNLOADED_MAPS_PATH =
      new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps");
  private final static File DEFAULT_SAVE_PATH = new File(ClientFileSystemHelper.getUserRootFolder(), "savedGames");


  @Override
  public void setToDefault() {
    setSaveGamePath(DEFAULT_SAVE_PATH.toString());
    setDownloadedMapPath(DEFAULT_DOWNLOADED_MAPS_PATH.toString());
  }

  public String getDownloadedMapPath() {
    // return the override first, then user maps folder preference, then the default
    return SystemPreferences.get(SystemPreferenceKey.MAP_FOLDER_OVERRIDE,
        SystemPreferences.get(SystemPreferenceKey.USER_MAPS_FOLDER_PATH, DEFAULT_DOWNLOADED_MAPS_PATH.toString()));
  }

  public void setDownloadedMapPath(final String downloadedMapPath) {
    SystemPreferences.put(SystemPreferenceKey.USER_MAPS_FOLDER_PATH, downloadedMapPath);
  }

  public String getSaveGamePath() {
    return SystemPreferences.get(SystemPreferenceKey.SAVE_GAMES_FOLDER_PATH, DEFAULT_SAVE_PATH.toString());
  }

  public void setSaveGamePath(final String saveGamePath) {
    SystemPreferences.put(SystemPreferenceKey.SAVE_GAMES_FOLDER_PATH, saveGamePath);
  }

}
