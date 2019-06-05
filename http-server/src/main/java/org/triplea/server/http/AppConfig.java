package org.triplea.server.http;

import static com.google.common.base.Preconditions.checkState;

import java.net.URI;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import lombok.Getter;
import lombok.Setter;


/**
 * This configuration class is injected with properties from the server YML configuration.
 * We also store here any other static configuration properties, all configuration
 * values should essentially live here and be obtained from this class. An instance of
 * this class is created by DropWizard on launch and then is passed to the application class.
 * The second startup argument is the property file to be loaded, with this we can specify
 * whether to use prerelease or production configuration by specifying the appropriate file.
 * In the YML files secret or sensitive values are defined by environment variables.
 */
public class AppConfig extends Configuration {
  public static final String GITHUB_ORG = "triplea-game";
  public static final URI GITHUB_WEB_SERVICE_API_URL = URI.create("https://api.github.com");
  public static final int MAX_ERROR_REPORTS_PER_DAY = 5;

  /**
   * Webservice token, should be an API token for the TripleA builder bot account.
   */
  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private String githubApiToken;

  /**
   * Repo where we will create github issues from bug report uploads.
   */
  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private String githubRepo;

  /**
   * Flag that indicates if we are running in production. This can be used to verify we do not have
   * any magic stubbing and to do any additional configuration checks to be really sure production is
   * well configured. Because we vary configuration values between prod and test, there can be prod-only
   * cases where we perhaps have something misconfigured, hence the risk we are trying to defend against.
   */
  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private boolean prod;

  @Valid
  @NotNull
  @JsonProperty
  @Getter
  private DataSourceFactory database = new DataSourceFactory();

  /**
   * Ensures that we have defined all values needed for running in production.
   * To allow environment variables to be defaulted, we have to configure drop wizard to allow
   * empty environment variable values. This opens up a vulnerability where in production we could
   * incorrectly define, or forget to define an environment variable value. In that case we'd launch
   * with an incorrect or incomplete configuration and might not know until that functionality is
   * exercised.
   */
  void verifyProdEnvironmentVariables() {
    checkState(prod);

    checkEnvVariable(githubApiToken, "GITHUB_API_TOKEN");
    checkEnvVariable(System.getenv("POSTGRES_PASSWORD"), "POSTGRES_PASSWORD");
  }

  private void checkEnvVariable(final String value, final String envVariableName) {
    checkState(
        value != null && !value.isEmpty(),
        envVariableName + " environment variable must be set");
  }
}
