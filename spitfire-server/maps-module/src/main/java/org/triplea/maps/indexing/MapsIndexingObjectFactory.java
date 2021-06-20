package org.triplea.maps.indexing;

import java.net.URI;
import java.time.Instant;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.maps.MapsModuleConfig;

@UtilityClass
public class MapsIndexingObjectFactory {
  /**
   * Factory method to create indexing task on a schedule. This does not start indexing, the
   * 'start()' method must be called for map indexing to begin.
   */
  public static MapsIndexingSchedule buildMapsIndexingSchedule(
      final MapsModuleConfig configuration, final Jdbi jdbi) {
    final var githubApiClient =
        GithubApiClient.builder()
            .uri(URI.create(configuration.getGithubWebServiceUrl()))
            .authToken(configuration.getGithubApiToken())
            .build();

    return new MapsIndexingSchedule(
        configuration.getMapIndexingPeriodMinutes(),
        MapIndexingTaskRunner.builder()
            .githubOrgName(configuration.getGithubMapsOrgName())
            .githubApiClient(githubApiClient)
            .mapIndexer(
                new MapIndexingTask(
                    lastCommitDateFetcher(githubApiClient, configuration.getGithubMapsOrgName())))
            .mapIndexDao(jdbi.onDemand(MapIndexDao.class))
            .indexingTaskDelaySeconds(configuration.getIndexingTaskDelaySeconds())
            .build());
  }

  /**
   * Returns function that can fetch the last commit date for a given repository specified by name.
   */
  static Function<String, Instant> lastCommitDateFetcher(
      final GithubApiClient githubApiClient, final String githubOrgName) {
    return githubRepoName ->
        githubApiClient
            .fetchBranchInfo(githubOrgName, githubRepoName, "master")
            .getLastCommitDate();
  }
}
