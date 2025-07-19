package games.strategy.engine.framework.map.download;

import static games.strategy.engine.framework.map.download.DownloadMapsWindow.TEXT_ABBREVIATION_NOT_APPLICABLE;

import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.io.FileUtils;
import org.triplea.java.Interruptibles;

/** Various logic methods used by DownloadMapsWindows. */
@Slf4j
class DownloadMapsWindowModel {
  private final InstalledMapsListing installedMapsListing;

  DownloadMapsWindowModel() {
    installedMapsListing = InstalledMapsListing.parseMapFiles();
  }

  public boolean isInstalled(final MapDownloadItem mapDownloadItem) {
    return getInstallLocation(mapDownloadItem).isPresent();
  }

  /** File reference for where to install the file, empty if not installed. */
  Optional<Path> getInstallLocation(final MapDownloadItem mapDownloadItem) {
    return installedMapsListing.findMapFolderByName(mapDownloadItem.getMapName());
  }

  public Optional<@NonNls String> getByteSizeTextForMap(final MapDownloadItem mapDownloadItem) {
    return getInstallLocation(mapDownloadItem)
        .map(
            mapPath -> {
              @NonNls String byteSizeText;
              try {
                byteSizeText =
                    org.apache.commons.io.FileUtils.byteCountToDisplaySize(
                        org.triplea.io.FileUtils.getByteSizeFromPath(mapPath));
              } catch (final IOException e) {
                log.warn("Failed to read file size", e);
                byteSizeText = TEXT_ABBREVIATION_NOT_APPLICABLE;
              }
              return MessageFormat.format(
                  "({0})<br>Path: {1}", byteSizeText, mapPath.toAbsolutePath());
            });
  }

  boolean delete(final MapDownloadItem mapDownloadItem) {
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

  String toHtmlString(final MapDownloadItem mapDownloadItem) {
    @NonNls String text = "<h1>" + mapDownloadItem.getMapName() + "</h1>\n";
    if (!mapDownloadItem.getPreviewImageUrl().isEmpty()) {
      text += "<img src='" + mapDownloadItem.getPreviewImageUrl() + "' />\n";
    }
    text += mapDownloadItem.getDescription();
    return text;
  }
}
