package org.triplea.maps.indexing;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.http.client.github.MapRepoListing;
import org.triplea.java.timer.Timers;

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
class MapIndexingTaskRunner implements Runnable {

  @Nonnull private final MapIndexDao mapIndexDao;
  @Nonnull private final GithubApiClient githubApiClient;
  @Nonnull private final Function<MapRepoListing, Optional<MapIndexingResult>> mapIndexer;
  @Nonnull private final Integer indexingTaskDelaySeconds;

  private int totalNumberMaps;
  private long startTimeEpochMillis;
  private int mapsDeleted;
  private int mapsIndexed;

  @Override
  public void run() {
    log.info("Map indexing started, github org: {}", githubApiClient.getOrg());

    startTimeEpochMillis = System.currentTimeMillis();

    // get list of maps
    final Collection<MapRepoListing> mapUris =
        githubApiClient.listRepositories().stream()
            .sorted(Comparator.comparing(MapRepoListing::getName))
            .collect(Collectors.toList());

    totalNumberMaps = mapUris.size();

    // remove deleted maps
    mapsDeleted =
        mapIndexDao.removeMapsNotIn(
            mapUris.stream()
                .map(MapRepoListing::getUri)
                .map(URI::toString)
                .collect(Collectors.toList()));

    // start indexing - convert maps to index to a stack and then process that
    // stack at a fixed rate to avoid rate limits.
    final Deque<MapRepoListing> reposToIndex = new ArrayDeque<>(mapUris);
    indexNextMapRepo(reposToIndex);
  }

  /**
   * Recursive method to process a stack of map repo listings. On each iteration we wait a fixed
   * delay, we then pop an element, process it, and then repeat until the stack is empty.
   */
  private void indexNextMapRepo(final Deque<MapRepoListing> reposToIndex) {
    performIndexing(reposToIndex.pop());
    if (reposToIndex.isEmpty()) {
      notifyCompletion();
    } else {
      Timers.executeAfterDelay(
          indexingTaskDelaySeconds, TimeUnit.SECONDS, () -> indexNextMapRepo(reposToIndex));
    }
  }

  /**
   * Performs the actual indexing of a single map repo listing. Indexing is two parts, first we
   * reach out to the repo to gather indexing information, second we upsert that info into database.
   */
  private void performIndexing(final MapRepoListing mapRepoListing) {
    log.info("Indexing map: " + mapRepoListing.getName());
    mapIndexer
        .apply(mapRepoListing)
        .ifPresent(
            mapIndexResult -> {
              mapIndexDao.upsert(mapIndexResult);
              mapsIndexed++;
            });
  }

  private void notifyCompletion() {
    log.info(
        "Map indexing finished in {} ms, repos found: {}, repos with map.yml: {}, maps deleted: {}",
        (System.currentTimeMillis() - startTimeEpochMillis),
        totalNumberMaps,
        mapsIndexed,
        mapsDeleted);
  }
}
