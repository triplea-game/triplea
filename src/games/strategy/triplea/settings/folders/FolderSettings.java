package games.strategy.triplea.settings.folders;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.PreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

import java.io.File;

public class FolderSettings implements HasDefaults {

  private String downloadedMapPath;
  private String saveGamePath;

  public FolderSettings() {
    final File defaultPath = new File(ClientFileSystemHelper.getUserRootFolder(), "downloadedMaps");
    downloadedMapPath = SystemPreferences.get(ClientFileSystemHelper.class, PreferenceKey.USER_MAPS_FOLDER_PATH, defaultPath.toString());
  }

  @Override
  public void setToDefault() {
  }

  public String getDownloadedMapPath() {
    return downloadedMapPath;
  }

  public void setDownloadedMapPath(String downloadedMapPath) {
    this.downloadedMapPath = downloadedMapPath;
    SystemPreferences.put(ClientFileSystemHelper.class, PreferenceKey.USER_MAPS_FOLDER_PATH, downloadedMapPath);
  }

  public String getSaveGamePath() {
    return saveGamePath;
  }

  public void setSaveGamePath(String saveGamePath) {
    this.saveGamePath = saveGamePath;
  }

}
