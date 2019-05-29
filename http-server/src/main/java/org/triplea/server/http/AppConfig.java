package org.triplea.server.http;

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
  static final String GITHUB_ORG = "triplea-game";
  static final URI GITHUB_WEB_SERVICE_API_URL = URI.create("https://api.github.com");
  static final int MAX_ERROR_REPORTS_PER_DAY = 5;

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private String githubApiToken;

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private String githubRepo;

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private boolean prod;

  @Valid
  @NotNull
  @JsonProperty
  @Getter
  private DataSourceFactory database = new DataSourceFactory();
}
