package games.strategy.engine.framework.map.download;

import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.java.Interruptibles;

/** Various logic methods used by DownloadMapsWindows. */
@Slf4j
class DownloadMapsWindowUtils {
  private final InstalledMapsListing installedMapsListing;

  DownloadMapsWindowUtils() {
    installedMapsListing = InstalledMapsListing.parseMapFiles();
  }

  public boolean isInstalled(final MapDownloadListing mapDownloadListing) {
    return getInstallLocation(mapDownloadListing).isPresent();
  }

  /** File reference for where to install the file, empty if not installed. */
  Optional<Path> getInstallLocation(final MapDownloadListing mapDownloadListing) {
    return installedMapsListing.findMapFolderByName(mapDownloadListing.getMapName());
  }

  boolean delete(final MapDownloadListing mapDownloadListing) {
    final Path installLocation = getInstallLocation(mapDownloadListing).orElse(null);
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

  String toHtmlString(final MapDownloadListing mapDownloadListing) {
    String text = "<h1>" + mapDownloadListing.getMapName() + "</h1>\n";
    if (!mapDownloadListing.getPreviewImageUrl().isEmpty()) {
      text += "<img src='" + mapDownloadListing.getPreviewImageUrl() + "' />\n";
    }
    text += mapDownloadListing.getDescription();
    return text;
  }
}
