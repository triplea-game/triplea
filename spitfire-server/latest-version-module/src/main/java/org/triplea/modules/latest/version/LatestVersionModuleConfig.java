package org.triplea.modules.latest.version;

import javax.annotation.Nullable;

public interface LatestVersionModuleConfig {
  /**
   * Auth token that will be sent to Github for webservice calls. Can be empty, but if specified
   * must be valid (no auth token still works, but rate limits will be more restrictive).
   */
  @Nullable
  String getGithubApiToken();

  /** Github API webserivce URL. */
  String getGithubWebServiceUrl();

  String getGithubOrg();

  String getGithubRepo();
}
