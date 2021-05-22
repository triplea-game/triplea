package org.triplea.maps.indexing;

import io.dropwizard.lifecycle.Managed;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.java.timer.ScheduledTimer;
import org.triplea.java.timer.Timers;
import org.triplea.maps.server.http.MapsConfig;

/**
 * Given a map indexing task, creates a schedule to run the indexing and once started will run at a
 * fixed rate until stopped.
 */
@Slf4j
public class MapsIndexingSchedule implements Managed {

  private final ScheduledTimer taskTimer;

  MapsIndexingSchedule(final MapIndexingTask mapIndexingTask) {
    taskTimer =
        Timers.fixedRateTimer("thread-name")
            .period(10, TimeUnit.MINUTES)
            .delay(10, TimeUnit.SECONDS)
            .task(mapIndexingTask);
  }

  /**
   * Factory method to create indexing task on a schedule. This does not start indexing, the
   * 'start()' method must be called for map indexing to begin.
   */
  public static MapsIndexingSchedule build(final MapsConfig configuration, final Jdbi jdbi) {
    final var githubApiClient =
        GithubApiClient.builder()
            .uri(URI.create(configuration.getGithubApiUri()))
            .authToken(configuration.getGithubApiKey())
            .isTest(false)
            .build();

    return new MapsIndexingSchedule(
        MapIndexingTask.builder()
            .githubOrgName(configuration.getGithubMapsOrgName())
            .githubApiClient(githubApiClient)
            .mapIndexer(
                new MapIndexer(
                    repoName ->
                        githubApiClient
                            .fetchBranchInfo(
                                configuration.getGithubMapsOrgName(), repoName, "master")
                            .getLastCommitDate()))
            .mapIndexDao(jdbi.onDemand(MapIndexDao.class))
            .build());
  }

  @Override
  public void start() {
    log.info("Map indexing started");
    taskTimer.start();
  }

  @Override
  public void stop() {
    log.info("Map indexing stopped");
    taskTimer.cancel();
  }
}
