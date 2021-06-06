package org.triplea.maps;

public interface MapsModuleConfig {
  String getGithubMapsOrgName();

  String getGithubApiToken();

  String getGithubWebServiceUrl();

  int getMapIndexingPeriodMinutes();

  int getIndexingTaskDelaySeconds();
}
