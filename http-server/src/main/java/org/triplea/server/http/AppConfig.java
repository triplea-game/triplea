package org.triplea.server.http;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
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
class AppConfig extends Configuration {
  static final String GITHUB_ORG = "triplea-game";
  static final URI GITHUB_WEB_SERVICE_API_URL = URI.create("https://api.github.com");

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private String githubApiToken;

  @Getter(onMethod_ = {@JsonProperty})
  @Setter(onMethod_ = {@JsonProperty})
  private String githubRepo;
}
