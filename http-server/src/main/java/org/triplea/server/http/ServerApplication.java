package org.triplea.server.http;

import org.jdbi.v3.core.Jdbi;
import org.triplea.server.error.reporting.ErrorReportControllerFactory;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationFactory;
import org.triplea.server.moderator.toolbox.bad.words.BadWordControllerFactory;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Main entry-point for launching drop wizard HTTP server.
 * This class is responsible for configuring any Jersey plugins,
 * registering resources (controllers) and injecting those resources
 * with configuration properties from 'AppConfig'.
 */
public class ServerApplication extends Application<AppConfig> {

  private static final String[] DEFAULT_ARGS =
      new String[] {"server", "configuration-prerelease.yml"};

  /**
   * Main entry-point method, launches the drop-wizard http server.
   * If no args are passed then will use default values suitable for local development.
   */
  public static void main(final String[] args) throws Exception {
    final ServerApplication application = new ServerApplication();
    // if no args are provided then we will use default values.
    application.run(args.length == 0 ? DEFAULT_ARGS : args);
  }

  @Override
  public void initialize(final Bootstrap<AppConfig> bootstrap) {
    // This bootstrap will replace ${...} values in YML configuration with environment
    // variable values. Without it, all values in the YML configuration are treated as literals.
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
            new EnvironmentVariableSubstitutor(false)));

    // From: https://www.dropwizard.io/0.7.1/docs/manual/jdbi.html
    // By adding the JdbiExceptionsBundle to your application, Dropwizard will automatically unwrap any
    // thrown SQLException or DBIException instances. This is critical for debugging, since otherwise
    // only the common wrapper exception’s stack trace is logged.
    bootstrap.addBundle(new JdbiExceptionsBundle());
  }

  @Override
  public void run(final AppConfig configuration, final Environment environment) {
    environment.jersey().register(new ClientExceptionMapper());

    if (configuration.isProd()) {
      configuration.verifyProdEnvironmentVariables();
    }

    final JdbiFactory factory = new JdbiFactory();
    final Jdbi jdbi = factory.build(environment, configuration.getDatabase(), "postgresql-connection-pool");

    // register all endpoint handlers here:
    environment.jersey().register(ErrorReportControllerFactory.errorReportController(configuration, jdbi));
    environment.jersey().register(BadWordControllerFactory.badWordController(jdbi));
    environment.jersey().register(ApiKeyValidationFactory.apiKeyValidationController(jdbi));
  }
}
