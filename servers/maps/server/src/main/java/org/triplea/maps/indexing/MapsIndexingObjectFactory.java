package org.triplea.maps.indexing;

import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiPredicate;
import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.http.client.github.MapRepoListing;
import org.triplea.maps.MapsServerConfig;
import org.triplea.maps.indexing.tasks.CommitDateFetcher;
import org.triplea.maps.indexing.tasks.DownloadSizeFetcher;
import org.triplea.maps.indexing.tasks.MapDescriptionReader;
import org.triplea.maps.indexing.tasks.MapNameReader;
import org.triplea.maps.indexing.tasks.SkipMapIndexingCheck;
import org.triplea.server.lib.scheduled.tasks.ScheduledTask;

@UtilityClass
public class MapsIndexingObjectFactory {
  /**
   * Factory method to create indexing task on a schedule. This does not start indexing, the
   * 'start()' method must be called for map indexing to begin.
   */
  public static Managed buildMapsIndexingSchedule(
      final MapsServerConfig configuration, final Jdbi jdbi) {

    var githubApiClient = configuration.createGithubApiClient();

    return ScheduledTask.builder()
        .taskName("Map-Indexing")
        .delay(Duration.ofSeconds(10))
        .period(Duration.ofMinutes(configuration.getMapIndexingPeriodMinutes()))
        .task(
            MapIndexingTaskRunner.builder()
                .githubApiClient(githubApiClient)
                .mapIndexer(
                    mapIndexingTask(
                        githubApiClient,
                        new SkipMapIndexingCheck(jdbi.onDemand(MapIndexDao.class))))
                .mapIndexDao(jdbi.onDemand(MapIndexDao.class))
                .indexingTaskDelaySeconds(configuration.getIndexingTaskDelaySeconds())
                .build())
        .build();
  }

  MapIndexingTask mapIndexingTask(
      final GithubApiClient githubApiClient,
      final BiPredicate<MapRepoListing, Instant> skipMapIndexingCheck) {
    return MapIndexingTask.builder()
        .lastCommitDateFetcher(CommitDateFetcher.builder().githubApiClient(githubApiClient).build())
        .skipMapIndexingCheck(skipMapIndexingCheck)
        .mapNameReader(MapNameReader.builder().build())
        .mapDescriptionReader(new MapDescriptionReader())
        .downloadSizeFetcher(new DownloadSizeFetcher())
        .build();
  }
}
