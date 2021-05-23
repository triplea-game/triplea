package org.triplea.modules;

/**
 * Interface for configuration parameters, typically these values will come from the servers
 * 'configuration.yml' file.
 */
public interface LobbyModuleConfig {
  /**
   * Should return true if we are in a production environment. Useful if we need to stub out
   * behavior in non-prod environments.
   */
  boolean isProd();

  String getGithubApiToken();

  String getGithubWebServiceUrl();

  String getGithubOrgForErrorReports();

  String getGithubRepoForErrorReports();
}
