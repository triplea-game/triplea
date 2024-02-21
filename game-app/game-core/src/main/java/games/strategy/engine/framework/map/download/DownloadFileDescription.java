package games.strategy.engine.framework.map.download;

import com.google.common.base.MoreObjects;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.listing.MapDownloadListing;
import java.io.File;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.util.Version;

/**
 * This class represents the essential data for downloading a TripleA map. Where to get it, where to
 * install it, version, etc..
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Builder
@AllArgsConstructor
public final class DownloadFileDescription {
  @EqualsAndHashCode.Include private final String url;
  private final String description;
  private final String mapName;
  private final Version version;
  private final DownloadType downloadType;
  private final MapCategory mapCategory;
  private final String img;

  enum DownloadType {
    MAP,
    MAP_SKIN,
    MAP_TOOL
  }

  enum MapCategory {
    BEST("High Quality"),

    GOOD("Good Quality"),

    DEVELOPMENT("In Development"),

    EXPERIMENTAL("Experimental");

    final String outputLabel;

    MapCategory(final String label) {
      outputLabel = label;
    }

    @Override
    public String toString() {
      return outputLabel;
    }

    private static MapCategory fromString(final String category) {
      return Arrays.stream(values())
          .filter(mapCategory -> mapCategory.outputLabel.equalsIgnoreCase(category))
          .findAny()
          .orElse(EXPERIMENTAL);
    }
  }

  public static DownloadFileDescription ofMapDownloadListing(
      final MapDownloadListing mapDownloadListing) {
    return DownloadFileDescription.builder()
        .url(mapDownloadListing.getUrl())
        .description(mapDownloadListing.getDescription())
        .mapName(mapDownloadListing.getMapName())
        .version(new Version(mapDownloadListing.getVersion()))
        .downloadType(DownloadType.MAP)
        .mapCategory(MapCategory.fromString(mapDownloadListing.getMapCategory()))
        .img(mapDownloadListing.getPreviewImage())
        .build();
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

  /** Returns the name of the zip file. */
  String getMapZipFileName() {
    return (url != null && url.contains("/")) ? url.substring(url.lastIndexOf('/') + 1) : "";
  }

  /** File reference for where to install the file. */
  File getInstallLocation() {
    final String masterSuffix =
        getMapZipFileName().toLowerCase().endsWith("master.zip") ? "-master" : "";
    final String normalizedMapName =
        getMapName().toLowerCase().replace(' ', '_') + masterSuffix + ".zip";
    return new File(
        ClientFileSystemHelper.getUserMapsFolder() + File.separator + normalizedMapName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .addValue(url)
        .addValue(mapName)
        .addValue(version)
        .toString();
  }

  String toHtmlString() {
    String text = "<h1>" + getMapName() + "</h1>\n";
    if (!img.isEmpty()) {
      text += "<img src='" + img + "' />\n";
    }
    text += getDescription();
    return text;
  }
}
