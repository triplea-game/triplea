package games.strategy.engine.framework.map.download;

import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.http.client.maps.listing.MapDownloadListing;

@Getter
class DownloadMapsWindowMapsListing {

  private final List<MapDownloadListing> available = new ArrayList<>();
  private final List<MapDownloadListing> installed = new ArrayList<>();
  private final List<MapDownloadListing> outOfDate = new ArrayList<>();

  DownloadMapsWindowMapsListing(final Collection<MapDownloadListing> downloads) {
    this(downloads, InstalledMapsListing.parseMapFiles());
  }

  @VisibleForTesting
  DownloadMapsWindowMapsListing(
      final Collection<MapDownloadListing> downloads,
      final InstalledMapsListing installedMapsListing) {
    for (final MapDownloadListing download : downloads) {
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

  List<MapDownloadListing> getAvailableExcluding(final Collection<MapDownloadListing> excluded) {
    return available.stream().filter(not(excluded::contains)).collect(Collectors.toList());
  }

  List<MapDownloadListing> getOutOfDateExcluding(final Collection<MapDownloadListing> excluded) {
    return outOfDate.stream().filter(not(excluded::contains)).collect(Collectors.toList());
  }
}
