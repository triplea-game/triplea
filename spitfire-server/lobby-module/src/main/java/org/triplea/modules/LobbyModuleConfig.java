package org.triplea.modules;

import javax.annotation.Nullable;

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

  /** If enabled, we will send real API requests to github to create error reports. */
  boolean isErrorReportToGithubEnabled();

  /**
   * Auth token that will be sent to Github for webservice calls. Can be empty, but if specified
   * must be valid (no auth token still works, but rate limits will be more restrictive).
   */
  @Nullable
  String getGithubApiToken();

  /** Github API webserivce URL. */
  String getGithubWebServiceUrl();

  String getGithubOrgForErrorReports();

  String getGithubRepoForErrorReports();
}
