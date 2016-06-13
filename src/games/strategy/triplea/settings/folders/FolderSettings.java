package games.strategy.triplea.settings.folders;

public class FolderSettings {

  private String downloadedMapPath;
  private String saveGamePath;

  public FolderSettings() {

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
