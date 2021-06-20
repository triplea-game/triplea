package games.strategy.engine.framework.map.download;

import com.google.common.base.MoreObjects;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
  @Nullable private final Path installLocation;

  boolean delete() {
    if (installLocation == null) {
      return true;
    }

    try {
      Files.delete(installLocation);
    } catch (final IOException e) {
      log.warn(
          "Unable to delete maps files.<br>Manual removal may be necessary: {}<br>{}",
          installLocation.toAbsolutePath(),
          e.getMessage(),
          e);
      return false;
    }

    // now sleep a short while before we check our work
    Interruptibles.sleep(10);
    if (Files.exists(installLocation)) {
      log.warn(
          "Unable to delete maps files.<br>Manual removal may be necessary: {}",
          installLocation.toAbsolutePath());
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
      final MapDownloadListing mapDownloadListing,
      final InstalledMapsListing installedMapsListing) {
    return DownloadFileDescription.builder()
        .url(mapDownloadListing.getUrl())
        .mapName(mapDownloadListing.getMapName())
        .description(mapDownloadListing.getDescription())
        // TODO: PROJECT#17 replace with latest commit date
        .version(1)
        .mapCategory(MapCategory.fromString(mapDownloadListing.getMapCategory()))
        .installLocation(
            installedMapsListing.findMapFolderByName(mapDownloadListing.getMapName()).orElse(null))
        .build();
  }

  /** File reference for where to install the file, empty if not installed. */
  Optional<Path> getInstallLocation() {
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
