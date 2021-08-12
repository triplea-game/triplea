package games.strategy.engine.framework.map.download;

import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.http.client.maps.listing.MapDownloadItem;

@Getter
class DownloadMapsWindowMapsListing {

  private final List<MapDownloadItem> available = new ArrayList<>();
  private final List<MapDownloadItem> installed = new ArrayList<>();
  private final List<MapDownloadItem> outOfDate = new ArrayList<>();

  DownloadMapsWindowMapsListing(final Collection<MapDownloadItem> downloads) {
    this(downloads, InstalledMapsListing.parseMapFiles());
  }

  @VisibleForTesting
  DownloadMapsWindowMapsListing(
      final Collection<MapDownloadItem> downloads,
      final InstalledMapsListing installedMapsListing) {
    for (final MapDownloadItem download : downloads) {
      if (download == null) {
        return;
      }

      if (!installedMapsListing.isMapInstalled(download.getMapName())) {
        available.add(download);
      } else {
        final int mapVersion = installedMapsListing.getMapVersionByName(download.getMapName());
        if (download.getVersion() != null && download.getVersion() > mapVersion) {
          outOfDate.add(download);
        } else {
          installed.add(download);
        }
      }
    }
  }

  List<MapDownloadItem> getAvailableExcluding(final Collection<MapDownloadItem> excluded) {
    return available.stream().filter(not(excluded::contains)).collect(Collectors.toList());
  }

  List<MapDownloadItem> getOutOfDateExcluding(final Collection<MapDownloadItem> excluded) {
    return outOfDate.stream().filter(not(excluded::contains)).collect(Collectors.toList());
  }
}
