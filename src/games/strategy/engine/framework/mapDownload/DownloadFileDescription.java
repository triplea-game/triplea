package games.strategy.engine.framework.mapDownload;


import java.io.File;
import java.net.URL;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.util.Version;

/**
 * This class represents the essential data for downloading a TripleA map. Where to get it, where to install it,
 * version, etc..
 */
public class DownloadFileDescription {
  protected static final String DUMMY_URL = "!";
  private final String url;
  private final String description;
  private final String mapName;
  private final Version version;
  private final DownloadType downloadType;

  public enum DownloadType {
    DISPLAY_HEADER, MAP, MAP_MOD, MAP_SKIN, MAP_TOOL
  }

  public static final DownloadFileDescription PLACE_HOLDER =
      new DownloadFileDescription(DUMMY_URL, " ", " ", new Version("0"), DownloadType.DISPLAY_HEADER);

  public DownloadFileDescription(final String url, final String description, final String mapName,
      final Version version, DownloadType downloadType) {
    super();
    this.url = url;
    this.description = description;
    this.mapName = mapName;
    this.version = version;
    this.downloadType = downloadType;
  }

  public String getUrl() {
    return url;
  }

  public String getDescription() {
    return description;
  }

  public String getMapName() {
    return mapName;
  }

  public boolean isDummyUrl() {
    return url.startsWith(DUMMY_URL);
  }

  public Version getVersion() {
    return version;
  }

  public URL newURL() {
    return DownloadUtils.toURL(url);
  }

  public boolean isMap() {
    return downloadType == DownloadType.MAP;
  }

  public boolean isMapMod() {
    return downloadType == DownloadType.MAP_MOD;
  }

  public boolean isMapSkin() {
    return downloadType == DownloadType.MAP_SKIN;
  }

  public boolean isMapTool() {
    return downloadType == DownloadType.MAP_TOOL;
  }

  /** @return Name of the zip file */
  public String getMapZipFileName() {
    if (url.contains("/")) {
      return url.substring(url.lastIndexOf('/') + 1, url.length());
    } else {
      return "";
    }
  }

  /** Translates the stored URL into a github new issue link */
  public String getFeedbackUrl() {
    if (url.contains("github.com") && url.contains("/releases/")) {
      return url.substring(0, url.indexOf("/releases/")) + "/issues/new";
    } else {
      return "";
    }
  }

  /** File reference for where to install the file */
  public File getInstallLocation() {
    return new File(ClientFileSystemHelper.getUserMapsFolder() + File.separator + getMapZipFileName());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).addValue(url).addValue(mapName).addValue(version).toString();
  }

  @Override
  public boolean equals(Object rhs) {
    if (rhs == null || getClass() != rhs.getClass()) {
      return false;
    }
    final DownloadFileDescription other = (DownloadFileDescription) rhs;
    return Objects.equal(this.url, other.url);
  }

}
