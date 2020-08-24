package org.triplea.maps.server.http;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.triplea.dropwizard.common.ServerConfiguration;

public class MapsServer extends Application<MapsConfig> {

  private static final String[] DEFAULT_ARGS = new String[] {"server", "configuration.yml"};

  /**
   * Main entry-point method, launches the drop-wizard server. If no args are passed then will use
   * default values suitable for at least local development.
   */
  public static void main(final String[] args) throws Exception {
    // if no args are provided then we will use default values.
    new MapsServer().run(args.length == 0 ? DEFAULT_ARGS : args);
  }

  @Override
  public void initialize(final Bootstrap<MapsConfig> bootstrap) {
    final ServerConfiguration<MapsConfig> serverConfiguration =
        new ServerConfiguration<>(bootstrap);
    serverConfiguration.enableEnvironmentVariablesInConfig();
    serverConfiguration.enableBetterJdbiExceptions();
    serverConfiguration.enableEndpointRateLimiting();
  }

  @Override
  public void run(final MapsConfig configuration, final Environment environment) throws Exception {}
}
