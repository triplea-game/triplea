package games.strategy.triplea.settings.folders;

import java.io.File;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.SystemPreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class FolderSettings implements HasDefaults {

  private static final File DEFAULT_DOWNLOADED_MAPS_PATH =
      new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps");
  private static final File DEFAULT_SAVE_PATH = new File(ClientFileSystemHelper.getUserRootFolder(), "savedGames");


  @Override
  public void setToDefault() {
    setSaveGamePath(DEFAULT_SAVE_PATH.toString());
    setDownloadedMapPath(DEFAULT_DOWNLOADED_MAPS_PATH.toString());
  }

  public String getDownloadedMapPath() {
    if (validPathFromSystemProperty(SystemPreferenceKey.MAP_FOLDER_OVERRIDE)) {
      return SystemPreferences.get(SystemPreferenceKey.MAP_FOLDER_OVERRIDE, "");
    } else if (validPathFromSystemProperty(SystemPreferenceKey.USER_MAPS_FOLDER_PATH)) {
      return SystemPreferences.get(SystemPreferenceKey.USER_MAPS_FOLDER_PATH, "");
    } else {
      return DEFAULT_DOWNLOADED_MAPS_PATH.toString();
    }
  }

  private static boolean validPathFromSystemProperty(SystemPreferenceKey systemProperty) {
    String value = SystemPreferences.get(systemProperty, "");
    return !value.isEmpty() && new File(value).exists();
  }


  void setDownloadedMapPath(final String downloadedMapPath) {
    SystemPreferences.put(SystemPreferenceKey.USER_MAPS_FOLDER_PATH, downloadedMapPath);
  }

  public String getSaveGamePath() {
    return SystemPreferences.get(SystemPreferenceKey.SAVE_GAMES_FOLDER_PATH, DEFAULT_SAVE_PATH.toString());
  }

  void setSaveGamePath(final String saveGamePath) {
    SystemPreferences.put(SystemPreferenceKey.SAVE_GAMES_FOLDER_PATH, saveGamePath);
  }

}
