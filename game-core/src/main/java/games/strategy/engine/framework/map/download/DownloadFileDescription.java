package games.strategy.engine.framework.map.download;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.primitives.Ints;
import games.strategy.engine.ClientFileSystemHelper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.io.CloseableDownloader;
import org.triplea.io.ContentDownloader;

/**
 * This class represents the essential data for downloading a TripleA map. Where to get it, where to
 * install it, version, etc..
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Builder
@AllArgsConstructor
@Slf4j
public final class DownloadFileDescription {
  @EqualsAndHashCode.Include private final String url;
  private final String mapName;
  private final Integer version;
  private final MapCategory mapCategory;

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
        .mapName(mapDownloadListing.getMapName())
        .version(Ints.tryParse(mapDownloadListing.getVersion()))
        .mapCategory(MapCategory.fromString(mapDownloadListing.getMapCategory()))
        .build();
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
    final String previewUrl = url + "/preview.png";
    return "<h1>"
        + getMapName()
        + "</h1>\n"
        + "<img src='"
        + previewUrl
        + "' />\n"
        + downloadDescription();
  }

  private String downloadDescription() {
    final String descriptionUri = url + "/description.html";
    try (CloseableDownloader downloader = new ContentDownloader(URI.create(descriptionUri))) {
      return IOUtils.toString(downloader.getStream(), Charsets.UTF_8);
    } catch (final IOException e) {
      log.warn(
          "Failed to read map description file at: {}\nError: {}", descriptionUri, e.getMessage());
      return "";
    }
  }
}
