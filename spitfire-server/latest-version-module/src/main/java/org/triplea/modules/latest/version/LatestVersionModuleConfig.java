package org.triplea.modules.latest.version;

import org.triplea.http.client.github.GithubApiClient;

public interface LatestVersionModuleConfig {
  GithubApiClient createGamesRepoGithubApiClient();
}
