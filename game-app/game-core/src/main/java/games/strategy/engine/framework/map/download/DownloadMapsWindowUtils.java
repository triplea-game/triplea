package games.strategy.engine.framework.map.download;

import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;

/** Various logic methods used by DownloadMapsWindows. */
@Slf4j
class DownloadMapsWindowUtils {
  private final InstalledMapsListing installedMapsListing;

  DownloadMapsWindowUtils() {
    installedMapsListing = InstalledMapsListing.parseMapFiles();
  }

  public boolean isInstalled(final DownloadFileDescription downloadFileDescription) {
    return getInstallLocation(downloadFileDescription).isPresent();
  }

  /** File reference for where to install the file, empty if not installed. */
  Optional<Path> getInstallLocation(final DownloadFileDescription downloadFileDescription) {
    return installedMapsListing.findMapFolderByName(downloadFileDescription.getMapName());
  }

  boolean delete(final DownloadFileDescription downloadFileDescription) {
    final Path installLocation = getInstallLocation(downloadFileDescription).orElse(null);
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

  String toHtmlString(final DownloadFileDescription downloadFileDescription) {
    String text = "<h1>" + downloadFileDescription.getMapName() + "</h1>\n";
    if (!downloadFileDescription.getPreviewImageUrl().isEmpty()) {
      text += "<img src='" + downloadFileDescription.getPreviewImageUrl() + "' />\n";
    }
    text += downloadFileDescription.getDescription();
    return text;
  }
}
