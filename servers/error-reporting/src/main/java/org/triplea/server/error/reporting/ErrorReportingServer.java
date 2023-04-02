package org.triplea.server.error.reporting;

import io.dropwizard.Application;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.net.URI;
import org.jdbi.v3.core.Jdbi;
import org.triplea.dropwizard.common.ServerConfiguration;
import org.triplea.http.client.github.GithubApiClient;

public class ErrorReportingServer extends Application<ErrorReportingConfiguration> {
  private static final String[] DEFAULT_ARGS = new String[] {"server", "configuration.yml"};

  private ServerConfiguration<ErrorReportingConfiguration> serverConfiguration;

  /**
   * Main entry-point method, launches the drop-wizard http server. If no args are passed then will
   * use default values suitable for local development.
   */
  public static void main(String[] args) throws Exception {
    final ErrorReportingServer application = new ErrorReportingServer();
    // if no args are provided then we will use default values.
    application.run(args.length == 0 ? DEFAULT_ARGS : args);
  }

  @Override
  public void initialize(Bootstrap<ErrorReportingConfiguration> bootstrap) {
    serverConfiguration =
        ServerConfiguration.build(bootstrap)
            .enableBetterJdbiExceptions()
            .enableEnvironmentVariablesInConfig();
  }

  @Override
  public void run(ErrorReportingConfiguration configuration, Environment environment) {
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
    //
    //        MapsModuleRowMappers.rowMappers().forEach(jdbi::registerRowMapper);
    //        LobbyModuleRowMappers.rowMappers().forEach(jdbi::registerRowMapper);
    //
    //        if (configuration.isLogSqlStatements()) {
    //            JdbiLogging.registerSqlLogger(jdbi);
    //        }
    //
    //        if (configuration.isMapIndexingEnabled()) {
    //            environment
    //                    .lifecycle()
    //                    .manage(MapsIndexingObjectFactory.buildMapsIndexingSchedule(configuration,
    // jdbi));
    //            log.info(
    //                    "Map indexing is enabled to run every:"
    //                            + " {} minutes with one map indexing request every {} seconds",
    //                    configuration.getMapIndexingPeriodMinutes(),
    //                    configuration.getIndexingTaskDelaySeconds());
    //        } else {
    //            log.info("Map indexing is disabled");
    //        }
    //
    //        final LatestVersionModule latestVersionModule = new LatestVersionModule();
    //        if (configuration.isLatestVersionFetcherEnabled()) {
    //            log.info("Latest Engine Version Fetching running every 30 minutes");
    //            environment
    //                    .lifecycle()
    //                    .manage(
    //                            latestVersionModule.buildRefreshSchedule(
    //                                    configuration,
    //                                    LatestVersionModule.RefreshConfiguration.builder()
    //                                            .delay(Duration.ofSeconds(10L))
    //                                            .period(Duration.ofMinutes(30L))
    //                                            .build()));
    //        } else {
    //            log.info("Latest Engine Version Fetching is disabled");
    //        }
    //
    //        serverConfiguration.registerRequestFilter(
    //                environment, BannedPlayerFilter.newBannedPlayerFilter(jdbi));
    //
    //        final MetricRegistry metrics = new MetricRegistry();
    //        AuthenticationConfiguration.enableAuthentication(
    //                environment,
    //                metrics,
    //                ApiKeyAuthenticator.build(jdbi),
    //                new RoleAuthorizer(),
    //                AuthenticatedUser.class);
    //
    //        serverConfiguration.registerExceptionMappers(environment, List.of(new
    // IllegalArgumentMapper()));
    //
    //        final var sessionIsBannedCheck = SessionBannedCheck.build(jdbi);
    //        final var gameConnectionMessagingBus = new WebSocketMessagingBus();
    //
    //        GenericWebSocket.init(
    //                GameConnectionWebSocket.class, gameConnectionMessagingBus,
    // sessionIsBannedCheck);
    //
    //        final var playerConnectionMessagingBus = new WebSocketMessagingBus();
    //        GenericWebSocket.init(
    //                PlayerConnectionWebSocket.class, playerConnectionMessagingBus,
    // sessionIsBannedCheck);
    //
    //        final var chatters = Chatters.build();
    //        ChatMessagingService.build(chatters, jdbi).configure(playerConnectionMessagingBus);
    //
    //        final GameListing gameListing = GameListing.build(jdbi, playerConnectionMessagingBus);
    //        List.of(
    //                        // lobby module controllers
    //                        AccessLogController.build(jdbi),
    //                        BadWordsController.build(jdbi),
    //                        CreateAccountController.build(jdbi),
    //                        DisconnectUserController.build(jdbi, chatters,
    // playerConnectionMessagingBus),
    //                        ForgotPasswordController.build(configuration, jdbi),
    //                        GameChatHistoryController.build(jdbi),
    //                        GameHostingController.build(jdbi),
    //                        GameListingController.build(gameListing),
    //                        LobbyWatcherController.build(configuration, jdbi, gameListing),
    //                        LoginController.build(jdbi, chatters),
    //                        UsernameBanController.build(jdbi),
    //                        UserBanController.build(
    //                                jdbi, chatters, playerConnectionMessagingBus,
    // gameConnectionMessagingBus),
    //                        ErrorReportController.build(configuration, jdbi),
    //                        ModeratorAuditHistoryController.build(jdbi),
    //                        ModeratorsController.build(jdbi),
    //                        MuteUserController.build(chatters),
    //                        PlayerInfoController.build(jdbi, chatters, gameListing),
    //                        PlayersInGameController.build(gameListing),
    //                        RemoteActionsController.build(jdbi, gameConnectionMessagingBus),
    //                        UpdateAccountController.build(jdbi),
    //
    //                        // maps module controllers
    //                        MapsController.build(jdbi),
    //                        MapTagAdminController.build(jdbi),
    //                        LatestVersionController.build(latestVersionModule))
    //                .forEach(controller -> environment.jersey().register(controller));
  }
}
