package games.strategy.engine.framework.map.download;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.io.FileUtils;
import org.triplea.java.Interruptibles;

/// Data model for DownloadMapsWindows especially to centrally store
/// maps information used for each UI component.
@Slf4j
class DownloadMapsWindowModel {
  @Getter private final InstalledMapsListing installedMapsListing;
  @Getter private final ManagedMapStore mapStore;
  private final Supplier<InstalledMapsListing> installedMapsReader;

  DownloadMapsWindowModel(List<MapDownloadItem> availableDownloads) {
    this(availableDownloads, InstalledMapsListing::parseMapFiles);
  }

  @VisibleForTesting
  DownloadMapsWindowModel(
      List<MapDownloadItem> availableDownloads,
      Supplier<InstalledMapsListing> installedMapsReader) {
    this.installedMapsReader = installedMapsReader;
    installedMapsListing = installedMapsReader.get();
    mapStore = new ManagedMapStore();
    new DownloadMapsWindowMapsListing(availableDownloads, installedMapsListing, mapStore);
  }

  /// Whether the map belongs to the Installed tab of the window.
  boolean isInstalled(final ManagedMap map) {
    return switch (map.getMapStatus()) {
      case INSTALLED, UPDATE_AVAILABLE, REMOVING -> true;
      // an update replaces a map that is installed; a new map is not installed until it is done
      case DOWNLOADING -> getInstallLocation(map).isPresent();
      case AVAILABLE, ERROR -> false;
      default -> throw new IllegalStateException("Unknown map status: " + map.getMapStatus());
    };
  }

  /// File reference for where to install the file, empty if not installed.
  private Optional<Path> getInstallLocation(final ManagedMap mapDownloadItem) {
    return installedMapsListing.findMapFolderByName(mapDownloadItem.getMapName());
  }

  /// The status of the map once its download is cancelled: again to get, or installed in its old
  /// version.
  ManagedMapStatus statusWithoutDownload(final ManagedMap map) {
    if (getInstallLocation(map).isEmpty()) {
      return ManagedMapStatus.AVAILABLE;
    }
    return Optional.ofNullable(map.getInstalledMap())
            .filter(installed -> installed.isOutOfDate(map.getMapDownloadItem()))
            .isPresent()
        ? ManagedMapStatus.UPDATE_AVAILABLE
        : ManagedMapStatus.INSTALLED;
  }

  boolean delete(final ManagedMap mapDownloadItem) {
    // a map installed since the window opened is not in the listing read then
    final Path installLocation =
        getInstallLocation(mapDownloadItem)
            .or(() -> installedMapsReader.get().findMapFolderByName(mapDownloadItem.getMapName()))
            .orElse(null);
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
      mapStore.updateStatus(List.of(mapDownloadItem), ManagedMapStatus.AVAILABLE);
      return true;
    }
  }
}
