package games.strategy.engine.framework.map.download;

import java.io.File;
import java.util.Objects;

import com.google.common.base.MoreObjects;

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


  enum DownloadType {
    MAP, MAP_SKIN, MAP_TOOL
  }


  enum MapCategory {
    BEST("High Quality"),

    GOOD("Good Quality"),

    DEVELOPMENT("In Development"),

    EXPERIMENTAL("Experimental");

    String outputLabel;


    MapCategory(final String label) {
      outputLabel = label;
    }

    @Override
    public String toString() {
      return outputLabel;
    }

  }

  DownloadFileDescription(final String url, final String description, final String mapName,
      final Version version, final DownloadType downloadType, final MapCategory mapCategory) {
    this(url, description, mapName, version, downloadType, mapCategory, "");
  }

  DownloadFileDescription(final String url, final String description, final String mapName,
      final Version version, final DownloadType downloadType, final MapCategory mapCategory, final String img) {
    this.url = url;
    this.description = description;
    this.mapName = mapName;
    this.version = version;
    this.downloadType = downloadType;
    this.mapCategory = mapCategory;
    this.img = img;
  }

  String getUrl() {
    return url;
  }

  String getDescription() {
    return description;
  }

  String getMapName() {
    return mapName;
  }

  Version getVersion() {
    return version;
  }

  MapCategory getMapCategory() {
    return mapCategory;
  }

  boolean isMap() {
    return downloadType == DownloadType.MAP;
  }

  boolean isMapSkin() {
    return downloadType == DownloadType.MAP_SKIN;
  }

  boolean isMapTool() {
    return downloadType == DownloadType.MAP_TOOL;
  }

  /**
   * @return Name of the zip file.
   */
  String getMapZipFileName() {
    return ((url != null) && url.contains("/")) ? url.substring(url.lastIndexOf('/') + 1, url.length()) : "";
  }

  /** File reference for where to install the file. */
  File getInstallLocation() {
    final String masterSuffix = (getMapZipFileName().toLowerCase().endsWith("master.zip")) ? "-master" : "";
    final String normalizedMapName = getMapName().toLowerCase().replace(' ', '_') + masterSuffix + ".zip";
    return new File(ClientFileSystemHelper.getUserMapsFolder() + File.separator + normalizedMapName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).addValue(url).addValue(mapName).addValue(version).toString();
  }

  String toHtmlString() {
    String text = "<h1>" + getMapName() + "</h1>\n";
    if (!img.isEmpty()) {
      text += "<img src='" + img + "' />\n";
    }
    text += getDescription();
    return text;
  }


  @Override
  public boolean equals(final Object rhs) {
    if ((rhs == null) || (getClass() != rhs.getClass())) {
      return false;
    }
    final DownloadFileDescription other = (DownloadFileDescription) rhs;
    return Objects.equals(this.url, other.url);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(url);
  }
}
