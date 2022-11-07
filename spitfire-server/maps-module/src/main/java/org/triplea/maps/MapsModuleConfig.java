package org.triplea.maps;

import org.triplea.http.client.github.GithubApiClient;

public interface MapsModuleConfig {
  GithubApiClient createMapsRepoGithubApiClient();

  int getMapIndexingPeriodMinutes();

  int getIndexingTaskDelaySeconds();
}
