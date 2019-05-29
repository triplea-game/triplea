package org.triplea.server.http;

import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.server.error.reporting.CreateIssueStrategy;
import org.triplea.server.error.reporting.ErrorReportController;
import org.triplea.server.error.reporting.ErrorReportGateKeeper;
import org.triplea.server.error.reporting.ErrorReportResponseConverter;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Main entry-point for launching drop wizard HTTP server.
 * This class is responsible for configuring any Jersey plugins,
 * registering resources (controllers) and injecting those resources
 * with configuration properties from 'AppConfig'.
 */
class ServerApplication extends Application<AppConfig> {

  public static void main(final String[] args) throws Exception {
    // if no args are provided then we will use default values.
    new ServerApplication().run(
        args.length == 0 ? new String[] {"server", "configuration-prerelease.yml"} : args);
  }

  @Override
  public void initialize(final Bootstrap<AppConfig> bootstrap) {
    // This bootstrap will replace ${...} values in YML configuration with environment
    // variable values. Without it, all values in the YML configuration are treated as literals.
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
            new EnvironmentVariableSubstitutor()));
  }

  @Override
  public void run(final AppConfig configuration, final Environment environment) {
    environment.jersey().register(new ClientExceptionMapper());

    // register all endpoint handlers here:
    environment.jersey().register(errorReportController(configuration));
  }

  private static ErrorReportController errorReportController(final AppConfig configuration) {
    final GithubIssueClient githubIssueClient = GithubIssueClient.builder()
        .uri(AppConfig.GITHUB_WEB_SERVICE_API_URL)
        .authToken(configuration.getGithubApiToken())
        .githubOrg(AppConfig.GITHUB_ORG)
        .githubRepo(configuration.getGithubRepo())
        .build();

    return new ErrorReportController(CreateIssueStrategy.builder()
        .githubIssueClient(githubIssueClient)
        .allowErrorReport(new ErrorReportGateKeeper())
        .responseAdapter(new ErrorReportResponseConverter())
        .build());
  }
}
