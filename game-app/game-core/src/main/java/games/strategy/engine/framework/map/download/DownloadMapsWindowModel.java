package games.strategy.engine.framework.map.download;

import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.io.FileUtils;
import org.triplea.java.Interruptibles;

/** Various logic methods used by DownloadMapsWindows. */
@Slf4j
class DownloadMapsWindowModel {
  private final InstalledMapsListing installedMapsListing;

  DownloadMapsWindowModel() {
    installedMapsListing = InstalledMapsListing.parseMapFiles();
  }

  public boolean isInstalled(final ManagedMap mapDownloadItem) {
    return getInstallLocation(mapDownloadItem).isPresent();
  }

  /** File reference for where to install the file, empty if not installed. */
  Optional<Path> getInstallLocation(final ManagedMap mapDownloadItem) {
    return installedMapsListing.findMapFolderByName(mapDownloadItem.getMapName());
  }

  boolean delete(final ManagedMap mapDownloadItem) {
    final Path installLocation = getInstallLocation(mapDownloadItem).orElse(null);
    if (installLocation == null) {
      return true;
    }

    try {
      FileUtils.deleteDirectory(installLocation);
    } catch (final IOException e) {
      log.warn(
          "Unable to delete maps files.<br>Manual removal may be necessary: {}<br>{}",
          installLocation.toAbsolutePath(),
          e.getMessage(),
          e);
      return false;
    }
    installedMapsListing.deleteInstalledMapByName(mapDownloadItem.getMapName());

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

  String toHtmlString(final ManagedMap managedMap) {
    @NonNls String text = "<h1>" + managedMap.getMapName() + "</h1>\n";
    MapDownloadItem mapDownloadItem = managedMap.getMapDownloadItem();
    if (!mapDownloadItem.getPreviewImageUrl().isEmpty()) {
      text += "<img src='" + mapDownloadItem.getPreviewImageUrl() + "' />\n";
    }
    text += mapDownloadItem.getDescription();
    return text;
  }
}
