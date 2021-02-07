package games.strategy.engine.framework.map.download;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Ints;
import games.strategy.engine.framework.map.file.system.loader.DownloadedMaps;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.java.Interruptibles;

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
  private final String description;
  private final String mapName;
  private final Integer version;
  private final MapCategory mapCategory;
  private final String img;
  @Nullable private final File installLocation;

  boolean delete() {
    if (installLocation == null) {
      return true;
    }

    try {
      Files.delete(installLocation.toPath());
    } catch (final IOException e) {
      log.warn(
          "Unable to delete maps files.<br>Manual removal may be necessary: {}<br>{}",
          installLocation.getAbsolutePath(),
          e.getMessage(),
          e);
      return false;
    }

    // now sleep a short while before we check our work
    Interruptibles.sleep(10);
    if (installLocation.exists()) {
      log.warn(
          "Unable to delete maps files.<br>Manual removal may be necessary: {}",
          installLocation.getAbsolutePath());
      return false;
    } else {
      return true;
    }
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
      final MapDownloadListing mapDownloadListing, final DownloadedMaps downloadedMaps) {
    return DownloadFileDescription.builder()
        .url(mapDownloadListing.getUrl())
        .mapName(mapDownloadListing.getMapName())
        .version(Ints.tryParse(mapDownloadListing.getVersion()))
        .mapCategory(MapCategory.fromString(mapDownloadListing.getMapCategory()))
        .installLocation(
            downloadedMaps.findMapFolderByName(mapDownloadListing.getMapName()).orElse(null))
        .build();
  }

  /** Returns the name of the zip file. */
  String getMapZipFileName() {
    return (url != null && url.contains("/")) ? url.substring(url.lastIndexOf('/') + 1) : "";
  }

  /** File reference for where to install the file, empty if not installed. */
  Optional<File> getInstallLocation() {
    return Optional.ofNullable(installLocation);
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
