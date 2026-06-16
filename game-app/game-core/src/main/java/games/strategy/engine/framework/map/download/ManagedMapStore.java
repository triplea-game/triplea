package games.strategy.engine.framework.map.download;

import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import lombok.Getter;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

enum StoreState {
  UNINITIALIZED,
  INITIALIZING,
  CHANGED,
  UP_TO_DATE
}

interface MapStatusListener {
  void mapStatusChanged(ManagedMapStatus oldStatus, ManagedMapStatus newStatus);
}

/// Central data model part to store for map information
final class ManagedMapStore implements DownloadListener {

  private final Map<String, ManagedMap> mapsByName = new HashMap<>(); // lower-case map name index
  private final Map<ManagedMapStatus, Set<ManagedMap>> groupsByStatus =
      new EnumMap<>(ManagedMapStatus.class);
  @Getter private StoreState storeState;
  private final List<MapStatusListener> mapStatusListeners = new ArrayList<>();

  void addMapStatusListener(MapStatusListener listener) {
    mapStatusListeners.add(listener);
  }

  ManagedMapStore() {
    storeState = StoreState.UNINITIALIZED;
    DownloadCoordinator.instance.addDownloadListener(this);
  }

  public void initialize(
      Collection<MapDownloadItem> downloads, InstalledMapsListing installedMapsListing) {
    storeState = StoreState.INITIALIZING;
    for (ManagedMapStatus mapStatus : ManagedMapStatus.values()) {
      groupsByStatus.put(mapStatus, new HashSet<>());
    }
    downloads.forEach(
        downloadItem -> {
          Optional<InstalledMap> installedMapByName =
              installedMapsListing.findInstalledMapByName(downloadItem.getMapName());
          put(
              installedMapByName
                  .map(installedMap -> new ManagedMap(downloadItem, installedMap))
                  .orElseGet(() -> new ManagedMap(downloadItem)));
        });
    storeState = StoreState.UP_TO_DATE;
  }

  boolean isEmpty() {
    return mapsByName.isEmpty();
  }

  Collection<ManagedMap> getAll() {
    return Collections.unmodifiableCollection(mapsByName.values());
  }

  void put(final ManagedMap map) {
    mapsByName.put(map.getMapName().toLowerCase(), map);
    groupsByStatus.get(map.getMapStatus()).add(map);
  }

  void updateStatus(final List<ManagedMap> mapsToUpdate, final ManagedMapStatus newMapStatus) {
    storeState = StoreState.CHANGED;
    final Set<ManagedMapStatus> oldStatuses = new HashSet<>();
    mapsToUpdate.stream()
        .map(ManagedMap::getMapName)
        .map(String::toLowerCase)
        .map(mapsByName::get)
        .forEach(
            managedMap -> {
              ManagedMapStatus oldMapStatus = managedMap.getMapStatus();
              groupsByStatus.get(oldMapStatus).remove(managedMap);
              managedMap.setMapStatus(newMapStatus);
              groupsByStatus.get(newMapStatus).add(managedMap);
              oldStatuses.add(oldMapStatus);
            });
    oldStatuses.forEach(
        oldMapStatus ->
            mapStatusListeners.forEach(
                mapStatusListener ->
                    mapStatusListener.mapStatusChanged(oldMapStatus, newMapStatus)));
  }

  List<ManagedMap> getByStatus(final ManagedMapStatus... mapStatuses) {
    List<ManagedMapStatus> list = Arrays.stream(mapStatuses).toList();
    return mapsByName.values().stream()
        .filter(map -> list.contains(map.getMapStatus()))
        .sorted(Comparator.comparing(ManagedMap::getMapName))
        .toList();
  }

  boolean hasAnyMap(final ManagedMapStatus... mapStatuses) {
    for (ManagedMapStatus mapStatus : mapStatuses) {
      if (!groupsByStatus.get(mapStatus).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  int getCountByStatus(final ManagedMapStatus... mapStatuses) {
    return Arrays.stream(mapStatuses)
        .mapToInt(mapStatus -> groupsByStatus.get(mapStatus).size())
        .sum();
  }

  public List<ManagedMap> getMapsByName(Supplier<Collection<String>> mapNamesSupplier) {
    return mapNamesSupplier.get().stream()
        .map(String::toLowerCase)
        .map(mapsByName::get)
        .filter(Objects::nonNull)
        .toList();
  }

  public Optional<ManagedMap> getMapByName(String mapName) {
    return Optional.ofNullable(mapsByName.getOrDefault(mapName.toLowerCase(), null));
  }

  @Override
  public void downloadUpdated(MapDownloadItem download, long bytesReceived) {
    // currently nothing to do, but needed due to interface
  }

  @Override
  public void downloadComplete(MapDownloadItem download) {
    SwingUtilities.invokeLater(
        () -> updateStatus(List.of(getMapByMapDownloadItem(download)), ManagedMapStatus.INSTALLED));
  }

  ManagedMap getMapByMapDownloadItem(MapDownloadItem mapDownloadItem) {
    return mapsByName.get(mapDownloadItem.getMapName().toLowerCase());
  }
}
