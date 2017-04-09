package games.strategy.engine.framework.map.download;

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
  private final String url;
  private final String description;
  private final String mapName;
  private final Version version;
  private final DownloadType downloadType;
  private final MapCategory mapCategory;
  private final String img;


  public enum DownloadType {
    MAP, MAP_SKIN, MAP_TOOL
  }


  public enum MapCategory {
    BEST("High Quality"),
    GOOD("Good Quality"),
    DEVELOPMENT("In Development"),
    EXPERIMENTAL("Experimental");

    String outputLabel;


    MapCategory(String label) {
      outputLabel = label;
    }

    @Override
    public String toString() {
      return outputLabel;
    }

  }

  public DownloadFileDescription(final String url, final String description, final String mapName,
      final Version version, final DownloadType downloadType, final MapCategory mapCategory) {
    this(url,description, mapName, version, downloadType, mapCategory, "");
  }

  public DownloadFileDescription(final String url, final String description, final String mapName,
      final Version version, final DownloadType downloadType, final MapCategory mapCategory, final String img) {
    this.url = url;
    this.description = description;
    this.mapName = mapName;
    this.version = version;
    this.downloadType = downloadType;
    this.mapCategory = mapCategory;
    this.img = img;
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

  public Version getVersion() {
    return version;
  }

  public MapCategory getMapCategory() {
    return mapCategory;
  }

  public URL newURL() {
    if (url == null) {
      return null;
    }
    return DownloadUtils.toURL(url);
  }

  public boolean isMap() {
    return downloadType == DownloadType.MAP;
  }

  public boolean isMapSkin() {
    return downloadType == DownloadType.MAP_SKIN;
  }

  public boolean isMapTool() {
    return downloadType == DownloadType.MAP_TOOL;
  }

  /** @return Name of the zip file. */
  public String getMapZipFileName() {
    if (url != null && url.contains("/")) {
      return url.substring(url.lastIndexOf('/') + 1, url.length());
    } else {
      return "";
    }
  }

  /** Translates the stored URL into a github new issue link. */
  public String getFeedbackUrl() {
    if (url.contains("github.com") && url.contains("/releases/")) {
      return url.substring(0, url.indexOf("/releases/")) + "/issues/new";
    } else {
      return "";
    }
  }

  /** File reference for where to install the file. */
  public File getInstallLocation() {
    String masterSuffix = (getMapZipFileName().toLowerCase().endsWith("master.zip")) ?
        "-master" : "";
    String normalizedMapName = getMapName().toLowerCase().replace(' ', '_') + masterSuffix + ".zip";
    return new File(ClientFileSystemHelper.getUserMapsFolder() + File.separator + normalizedMapName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).addValue(url).addValue(mapName).addValue(version).toString();
  }

  public String toHtmlString() {
    String text = "<h1>" + getMapName() + "</h1>\n";
    if (!img.isEmpty()) {
      text += "<img src='" + img + "' />\n";
    }
    text += getDescription();
    return text;
  }


  @Override
  public boolean equals(final Object rhs) {
    if (rhs == null || getClass() != rhs.getClass()) {
      return false;
    }
    final DownloadFileDescription other = (DownloadFileDescription) rhs;
    return Objects.equal(this.url, other.url);
  }

  @Override
  public int hashCode() {
    return url.hashCode();
  }

}
