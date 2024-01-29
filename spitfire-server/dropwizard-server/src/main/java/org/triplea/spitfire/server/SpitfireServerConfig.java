package org.triplea.spitfire.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import java.net.URI;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.modules.LobbyModuleConfig;
import org.triplea.modules.latest.version.LatestVersionModuleConfig;

/**
 * This configuration class represents the configuration values in the server YML configuration. An
 * instance of this class is created by DropWizard on launch and then is passed to the application
 * class. Values can be injected into the application by using environment variables in the server
 * YML configuration file.
 */
public class SpitfireServerConfig extends Configuration
    implements LobbyModuleConfig, LatestVersionModuleConfig {

  /**
   * Flag that indicates if we are running in production. This can be used to verify we do not have
   * any magic stubbing and to do any additional configuration checks to be really sure production
   * is well configured. Because we vary configuration values between prod and test, there can be
   * prod-only cases where we perhaps have something misconfigured, hence the risk we are trying to
   * defend against.
   */
  @Getter(onMethod_ = {@JsonProperty, @Override})
  @Setter(onMethod_ = {@JsonProperty})
  private boolean gameHostConnectivityCheckEnabled;

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private boolean logSqlStatements;

  @Valid @NotNull @JsonProperty @Getter
  private final DataSourceFactory database = new DataSourceFactory();

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private String githubWebServiceUrl;

  /** Webservice token, should be an API token for the TripleA builder bot account. */
  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private String githubApiToken;

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private boolean errorReportToGithubEnabled;

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private String githubGameOrg;

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private String githubGameRepo;

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private boolean latestVersionFetcherEnabled;

  @Override
  public GithubApiClient createGamesRepoGithubApiClient() {
    return GithubApiClient.builder()
        .stubbingModeEnabled(!errorReportToGithubEnabled)
        .authToken(githubApiToken)
        .uri(URI.create(githubWebServiceUrl))
        .org(githubGameOrg)
        .repo(githubGameRepo)
        .build();
  }
}
