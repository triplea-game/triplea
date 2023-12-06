package org.triplea.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;

@Data
public class GameSupportServerConfiguration extends Configuration {

  String githubApiToken;
  String githubWebServiceUrl;

  String githubGameOrg; // : triplea-game
  String githubGameRepo; // triplea
  String githubMapsOrgName; // : triplea-maps

  Boolean errorReportToGithubEnabled;

  @Valid @NotNull @JsonProperty @Getter DataSourceFactory database = new DataSourceFactory();
}
