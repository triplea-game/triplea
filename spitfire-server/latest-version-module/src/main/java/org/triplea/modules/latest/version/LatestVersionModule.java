package org.triplea.modules.latest.version;

import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.server.lib.scheduled.tasks.ScheduledTask;

@Slf4j
public class LatestVersionModule {

  private String latestVersion;

  public Optional<String> getLatestVersion() {
    return Optional.ofNullable(latestVersion);
  }

  public void notifyLatest(final String newLatestVersion) {
    log.info("Latest engine version set to: {}", newLatestVersion);
    this.latestVersion = newLatestVersion;
  }

  @Builder
  @Value
  public static class RefreshConfiguration {
    Duration delay;
    Duration period;
  }

  public Managed buildRefreshSchedule(
      final LatestVersionModuleConfig configuration,
      final RefreshConfiguration refreshConfiguration) {

    final GithubApiClient githubApiClient = configuration.createGamesRepoGithubApiClient();

    final Supplier<Optional<String>> githubLatestVersionFetcher =
        githubApiClient::fetchLatestVersion;

    return ScheduledTask.builder()
        .taskName("Latest-Engine-Version-Fetcher")
        .delay(refreshConfiguration.getDelay())
        .period(refreshConfiguration.getPeriod())
        .task(() -> githubLatestVersionFetcher.get().ifPresent(this::notifyLatest))
        .build();
  }
}
