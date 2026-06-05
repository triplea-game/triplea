package games.strategy.engine.framework.map.download;

import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

@Getter
class DownloadMapsWindowMapsListing {

  private final Collection<MapDownloadItem> available = new ArrayList<>();
  private final Map<MapDownloadItem, InstalledMap> installed = new HashMap<>();
  private final Map<MapDownloadItem, InstalledMap> outOfDate = new HashMap<>();

  DownloadMapsWindowMapsListing(
      final Collection<MapDownloadItem> downloads, final ManagedMapStore mapStoreExisting) {
    this(downloads, InstalledMapsListing.parseMapFiles(), mapStoreExisting);
  }

  @VisibleForTesting
  DownloadMapsWindowMapsListing(
      final Collection<MapDownloadItem> downloads,
      final InstalledMapsListing installedMapsListing) {
    this(downloads, installedMapsListing, null);
  }

  @VisibleForTesting
  DownloadMapsWindowMapsListing(
      final Collection<MapDownloadItem> downloads,
      final InstalledMapsListing installedMapsListing,
      @Nullable final ManagedMapStore mapStoreExisting) {

    final ManagedMapStore mapStore =
        mapStoreExisting != null ? mapStoreExisting : new ManagedMapStore();
    mapStore.initialize(downloads, installedMapsListing);

    for (ManagedMap map : mapStore.getAll()) {
      switch (map.getMapStatus()) {
        case AVAILABLE -> available.add(map.getMapDownloadItem());
        case INSTALLED -> installed.put(map.getMapDownloadItem(), map.getInstalledMap());
        case UPDATE_AVAILABLE -> {
          // currently outOfDate maps are to be shown as installed as well
          installed.put(map.getMapDownloadItem(), map.getInstalledMap());
          outOfDate.put(map.getMapDownloadItem(), map.getInstalledMap());
        }
      }
    }
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
