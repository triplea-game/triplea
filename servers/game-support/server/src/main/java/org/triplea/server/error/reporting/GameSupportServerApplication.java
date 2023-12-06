package org.triplea.server.error.reporting;

import io.dropwizard.Application;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.net.URI;
import org.jdbi.v3.core.Jdbi;
import org.triplea.dropwizard.common.ServerConfiguration;
import org.triplea.http.client.github.GithubApiClient;

public class GameSupportServerApplication extends Application<GameSupportServerConfiguration> {
  private static final String[] DEFAULT_ARGS = new String[] {"server", "configuration.yml"};

  private ServerConfiguration<GameSupportServerConfiguration> serverConfiguration;

  /**
   * Main entry-point method, launches the drop-wizard http server. If no args are passed then will
   * use default values suitable for local development.
   */
  public static void main(String[] args) throws Exception {
    final GameSupportServerApplication application = new GameSupportServerApplication();
    // if no args are provided then we will use default values.
    application.run(args.length == 0 ? DEFAULT_ARGS : args);
  }

  @Override
  public void initialize(Bootstrap<GameSupportServerConfiguration> bootstrap) {
    serverConfiguration =
        ServerConfiguration.build(bootstrap)
            .enableBetterJdbiExceptions()
            .enableEnvironmentVariablesInConfig();
  }

  @Override
  public void run(GameSupportServerConfiguration configuration, Environment environment) {
    serverConfiguration.enableEnvironmentVariablesInConfig();

    final Jdbi jdbi =
        new JdbiFactory()
            .build(environment, configuration.getDatabase(), "postgresql-connection-pool");

    var githubApiClient =
        GithubApiClient.builder()
            .authToken(configuration.getGithubApiToken())
            .uri(URI.create(configuration.getGithubWebServiceUrl()))
            .repo(configuration.getGithubGameRepo())
            .stubbingModeEnabled(!configuration.getErrorReportToGithubEnabled())
            .org(configuration.getGithubGameOrg())
            .build();

    environment.jersey().register(ErrorReportController.build(githubApiClient, jdbi));
  }
}
