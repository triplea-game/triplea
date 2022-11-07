package games.strategy.engine.framework.map.download;

import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.http.client.maps.listing.MapDownloadItem;

@Getter
class DownloadMapsWindowMapsListing {

  private final Collection<MapDownloadItem> available;
  private final Map<MapDownloadItem, InstalledMap> installed;
  private final Map<MapDownloadItem, InstalledMap> outOfDate;

  DownloadMapsWindowMapsListing(final Collection<MapDownloadItem> downloads) {
    this(downloads, InstalledMapsListing.parseMapFiles());
  }

  @VisibleForTesting
  DownloadMapsWindowMapsListing(
      final Collection<MapDownloadItem> downloads,
      final InstalledMapsListing installedMapsListing) {

    outOfDate = installedMapsListing.findOutOfDateMaps(downloads);
    installed = installedMapsListing.findInstalledMapsFromDownloadList(downloads);
    available = installedMapsListing.findNotInstalledMapsFromDownloadList(downloads);
  }

  List<MapDownloadItem> getAvailableExcluding(final Collection<MapDownloadItem> excluded) {
    return available.stream().filter(not(excluded::contains)).collect(Collectors.toList());
  }

  List<MapDownloadItem> getOutOfDateExcluding(final Collection<MapDownloadItem> excluded) {
    final Collection<MapDownloadItem> downloads = outOfDate.keySet();
    downloads.removeAll(excluded);
    return downloads.stream()
        .sorted(Comparator.comparing(MapDownloadItem::getMapName))
        .collect(Collectors.toList());
  }
}
