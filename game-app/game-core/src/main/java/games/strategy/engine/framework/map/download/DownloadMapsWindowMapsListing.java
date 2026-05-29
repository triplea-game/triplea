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
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

@Getter
class DownloadMapsWindowMapsListing {

  private final Collection<MapDownloadItem> available;
  private final Map<MapDownloadItem, InstalledMap> installed;
  private final Map<MapDownloadItem, InstalledMap> outOfDate;
  // @TODO: Complete redesign to contain model for maps replacing state holdings in UI components
  // in the future
  private final ManagedMapStore mapStore;

  DownloadMapsWindowMapsListing(final Collection<MapDownloadItem> downloads) {
    this(downloads, InstalledMapsListing.parseMapFiles());
  }

  @VisibleForTesting
  DownloadMapsWindowMapsListing(
      final Collection<MapDownloadItem> downloads,
      final InstalledMapsListing installedMapsListing) {

    mapStore = new ManagedMapStore();
    mapStore.initialize(downloads, installedMapsListing);

    available =
        mapStore.getByStatus(ManagedMapStatus.AVAILABLE).stream()
            .map(ManagedMap::getMapDownloadItem)
            .toList();
    outOfDate =
        mapStore.getByStatus(ManagedMapStatus.UPDATE_AVAILABLE).stream()
            .collect(Collectors.toMap(ManagedMap::getMapDownloadItem, ManagedMap::getInstalledMap));
    installed =
        mapStore.getByStatus(ManagedMapStatus.INSTALLED).stream()
            .collect(Collectors.toMap(ManagedMap::getMapDownloadItem, ManagedMap::getInstalledMap));
    installed.putAll(outOfDate); // currently outOfDate maps are to be shown as installed as well
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
