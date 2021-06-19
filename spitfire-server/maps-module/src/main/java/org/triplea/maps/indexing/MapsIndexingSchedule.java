package org.triplea.maps.indexing;

import io.dropwizard.lifecycle.Managed;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.java.timer.ScheduledTimer;
import org.triplea.java.timer.Timers;
import org.triplea.maps.MapsModuleConfig;

/**
 * Given a map indexing task, creates a schedule to run the indexing and once started will run at a
 * fixed rate until stopped.
 */
@Slf4j
public class MapsIndexingSchedule implements Managed {

  private final ScheduledTimer taskTimer;

  MapsIndexingSchedule(
      final int indexingPeriodMinutes, final MapIndexingTaskRunner mapIndexingTaskRunner) {
    taskTimer =
        Timers.fixedRateTimer("thread-name")
            .period(indexingPeriodMinutes, TimeUnit.MINUTES)
            .delay(10, TimeUnit.SECONDS)
            .task(mapIndexingTaskRunner);
  }

  /**
   * Factory method to create indexing task on a schedule. This does not start indexing, the
   * 'start()' method must be called for map indexing to begin.
   */
  public static MapsIndexingSchedule build(final MapsModuleConfig configuration, final Jdbi jdbi) {
    final var githubApiClient =
        GithubApiClient.builder()
            .uri(URI.create(configuration.getGithubWebServiceUrl()))
            .authToken(configuration.getGithubApiToken())
            .isTest(false)
            .build();

    return new MapsIndexingSchedule(
        configuration.getMapIndexingPeriodMinutes(),
        MapIndexingTaskRunner.builder()
            .githubOrgName(configuration.getGithubMapsOrgName())
            .githubApiClient(githubApiClient)
            .mapIndexer(
                new MapIndexingTask(
                    repoName ->
                        githubApiClient
                            .fetchBranchInfo(
                                configuration.getGithubMapsOrgName(), repoName, "master")
                            .getLastCommitDate()))
            .mapIndexDao(jdbi.onDemand(MapIndexDao.class))
            .indexingTaskDelaySeconds(configuration.getIndexingTaskDelaySeconds())
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
