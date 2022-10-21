package org.triplea.modules;

import org.triplea.http.client.github.GithubApiClient;

/**
 * Interface for configuration parameters, typically these values will come from the servers
 * 'configuration.yml' file.
 */
public interface LobbyModuleConfig {
  /**
   * When true we will do a reverse connectivity check to game hosts. Otherwise false will enable a
   * special test only endpoint that still does authentication but will not do the reverse
   * connectivity check.
   */
  boolean isGameHostConnectivityCheckEnabled();

  GithubApiClient createGamesRepoGithubApiClient();
}
