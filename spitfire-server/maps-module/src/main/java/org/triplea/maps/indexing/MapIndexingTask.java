package org.triplea.maps.indexing;

import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.http.client.github.MapRepoListing;

/**
 * Task that runs a map indexing pass on all maps. The indexing will update database to reflect the
 * latest checked in across all map repositories.
 *
 * <ul>
 *   <li>Queries Github for list of map repos
 *   <li>Checks each map repo for a 'map.yml' and reads the map name and version
 *   <li>Deletes from database maps that have been removed
 *   <li>Upserts latest map info into database
 * </ul>
 */
@Builder
@Slf4j
class MapIndexingTask implements Runnable {

  @Nonnull private final String githubOrgName;
  @Nonnull private final MapIndexDao mapIndexDao;
  @Nonnull private final GithubApiClient githubApiClient;
  @Nonnull private final Function<MapRepoListing, Optional<MapIndexResult>> mapIndexer;

  @Override
  public void run() {
    log.info("Map indexing started, github org: {}", githubOrgName);

    final long start = System.currentTimeMillis();

    // get list of maps
    final Collection<MapRepoListing> mapUris =
        githubApiClient.listRepositories(githubOrgName).stream()
            .sorted(Comparator.comparing(MapRepoListing::getName))
            .collect(Collectors.toList());

    // remove deleted maps
    final int mapsRemovedCount =
        mapIndexDao.removeMapsNotIn(
            mapUris.stream()
                .map(MapRepoListing::getUri)
                .map(URI::toString)
                .collect(Collectors.toList()));

    // index all maps
    final Collection<MapIndexResult> indexedMapData =
        mapUris.stream()
            .map(mapIndexer)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    // upsert indexed map data into DB
    indexedMapData.forEach(mapIndexDao::upsert);

    log.info(
        "Map indexing finished in {} ms, repos found: {}, repos with map.yml: {}, maps deleted: {}",
        (System.currentTimeMillis() - start),
        mapUris.size(),
        indexedMapData.size(),
        mapsRemovedCount);
  }
}
