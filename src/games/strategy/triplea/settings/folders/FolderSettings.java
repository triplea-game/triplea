package games.strategy.triplea.settings.folders;

import games.strategy.triplea.settings.HasDefaults;

public class FolderSettings implements HasDefaults {

  private String downloadedMapPath;
  private String saveGamePath;

  public FolderSettings() {

  }

  @Override
  public void setToDefault() {

  }

  public String getDownloadedMapPath() {
    return downloadedMapPath;
  }

  public void setDownloadedMapPath(String downloadedMapPath) {
    this.downloadedMapPath = downloadedMapPath;
  }

  public String getSaveGamePath() {
    return saveGamePath;
  }

  public void setSaveGamePath(String saveGamePath) {
    this.saveGamePath = saveGamePath;
  }

}
