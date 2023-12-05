package org.triplea.maps;

import io.dropwizard.Application;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.triplea.dropwizard.common.IllegalArgumentMapper;
import org.triplea.dropwizard.common.JdbiLogging;
import org.triplea.dropwizard.common.ServerConfiguration;
import org.triplea.maps.indexing.MapsIndexingObjectFactory;

/**
 * Main entry-point for launching drop wizard HTTP server. This class is responsible for configuring
 * any Jersey plugins, registering resources (controllers) and injecting those resources with
 * configuration properties from 'AppConfig'.
 */
@Slf4j
public class MapsServerApplication extends Application<MapsServerConfig> {

  private static final String[] DEFAULT_ARGS = new String[] {"server", "configuration.yml"};

  private ServerConfiguration<MapsServerConfig> serverConfiguration;

  /**
   * Main entry-point method, launches the drop-wizard http server. If no args are passed then will
   * use default values suitable for local development.
   */
  public static void main(final String[] args) throws Exception {
    final MapsServerApplication application = new MapsServerApplication();
    // if no args are provided then we will use default values.
    application.run(args.length == 0 ? DEFAULT_ARGS : args);
  }

  @Override
  public void initialize(final Bootstrap<MapsServerConfig> bootstrap) {
    serverConfiguration =
        ServerConfiguration.build(bootstrap)
            .enableEnvironmentVariablesInConfig()
            .enableBetterJdbiExceptions();
  }

  @Override
  public void run(final MapsServerConfig configuration, final Environment environment) {
    final Jdbi jdbi =
        new JdbiFactory()
            .build(environment, configuration.getDatabase(), "postgresql-connection-pool");

    MapsModuleRowMappers.rowMappers().forEach(jdbi::registerRowMapper);

    if (configuration.isLogSqlStatements()) {
      JdbiLogging.registerSqlLogger(jdbi);
    }

    if (configuration.isMapIndexingEnabled()) {
      environment
          .lifecycle()
          .manage(MapsIndexingObjectFactory.buildMapsIndexingSchedule(configuration, jdbi));
      log.info(
          "Map indexing is enabled to run every:"
              + " {} minutes with one map indexing request every {} seconds",
          configuration.getMapIndexingPeriodMinutes(),
          configuration.getIndexingTaskDelaySeconds());
    } else {
      log.info("Map indexing is disabled");
    }

    serverConfiguration.registerExceptionMappers(environment, List.of(new IllegalArgumentMapper()));

    List.of(MapsController.build(jdbi), MapTagAdminController.build(jdbi))
        .forEach(controller -> environment.jersey().register(controller));
  }
}
