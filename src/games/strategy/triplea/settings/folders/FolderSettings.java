package games.strategy.triplea.settings.folders;

import java.io.File;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.PreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class FolderSettings implements HasDefaults {


  private final static File DEFAULT_DOWNLOADED_MAPS_PATH =
      new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps");
  private final static File DEFAULT_SAVE_PATH = new File(ClientFileSystemHelper.getUserRootFolder(), "savedGames");

  public FolderSettings() {}


  @Override
  public void setToDefault() {
    setSaveGamePath(DEFAULT_SAVE_PATH.toString());
    setDownloadedMapPath(DEFAULT_DOWNLOADED_MAPS_PATH.toString());
  }

  public String getDownloadedMapPath() {
    return SystemPreferences.get(PreferenceKey.USER_MAPS_FOLDER_PATH, DEFAULT_DOWNLOADED_MAPS_PATH.toString());
  }

  public void setDownloadedMapPath(String downloadedMapPath) {
    SystemPreferences.put(PreferenceKey.USER_MAPS_FOLDER_PATH, downloadedMapPath);
  }

  public String getSaveGamePath() {
    return SystemPreferences.get(PreferenceKey.SAVE_GAMES_FOLDER_PATH, DEFAULT_SAVE_PATH.toString());
  }

  public void setSaveGamePath(String saveGamePath) {
    SystemPreferences.put(PreferenceKey.SAVE_GAMES_FOLDER_PATH, saveGamePath);
  }

}
