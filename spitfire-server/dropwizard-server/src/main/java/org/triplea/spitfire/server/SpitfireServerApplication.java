package org.triplea.spitfire.server;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import io.dropwizard.Application;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.LobbyModuleRowMappers;
import org.triplea.dropwizard.common.AuthenticationConfiguration;
import org.triplea.dropwizard.common.JdbiLogging;
import org.triplea.dropwizard.common.ServerConfiguration;
import org.triplea.dropwizard.common.ServerConfiguration.WebsocketConfig;
import org.triplea.http.client.web.socket.WebsocketPaths;
import org.triplea.maps.MapsModuleRowMappers;
import org.triplea.maps.indexing.MapsIndexingObjectFactory;
import org.triplea.modules.chat.ChatMessagingService;
import org.triplea.modules.chat.Chatters;
import org.triplea.modules.game.listing.GameListing;
import org.triplea.spitfire.server.access.authentication.ApiKeyAuthenticator;
import org.triplea.spitfire.server.access.authentication.AuthenticatedUser;
import org.triplea.spitfire.server.access.authorization.BannedPlayerFilter;
import org.triplea.spitfire.server.access.authorization.RoleAuthorizer;
import org.triplea.spitfire.server.controllers.ErrorReportController;
import org.triplea.spitfire.server.controllers.lobby.GameHostingController;
import org.triplea.spitfire.server.controllers.lobby.GameListingController;
import org.triplea.spitfire.server.controllers.lobby.LobbyWatcherController;
import org.triplea.spitfire.server.controllers.lobby.PlayersInGameController;
import org.triplea.spitfire.server.controllers.lobby.moderation.AccessLogController;
import org.triplea.spitfire.server.controllers.lobby.moderation.BadWordsController;
import org.triplea.spitfire.server.controllers.lobby.moderation.DisconnectUserController;
import org.triplea.spitfire.server.controllers.lobby.moderation.GameChatHistoryController;
import org.triplea.spitfire.server.controllers.lobby.moderation.ModeratorAuditHistoryController;
import org.triplea.spitfire.server.controllers.lobby.moderation.ModeratorsController;
import org.triplea.spitfire.server.controllers.lobby.moderation.MuteUserController;
import org.triplea.spitfire.server.controllers.lobby.moderation.RemoteActionsController;
import org.triplea.spitfire.server.controllers.lobby.moderation.UserBanController;
import org.triplea.spitfire.server.controllers.lobby.moderation.UsernameBanController;
import org.triplea.spitfire.server.controllers.user.account.CreateAccountController;
import org.triplea.spitfire.server.controllers.user.account.ForgotPasswordController;
import org.triplea.spitfire.server.controllers.user.account.LoginController;
import org.triplea.spitfire.server.controllers.user.account.PlayerInfoController;
import org.triplea.spitfire.server.controllers.user.account.UpdateAccountController;
import org.triplea.spitfire.server.maps.MapTagAdminController;
import org.triplea.spitfire.server.maps.MapsController;
import org.triplea.web.socket.GameConnectionWebSocket;
import org.triplea.web.socket.GenericWebSocket;
import org.triplea.web.socket.PlayerConnectionWebSocket;
import org.triplea.web.socket.SessionBannedCheck;
import org.triplea.web.socket.WebSocketMessagingBus;

/**
 * Main entry-point for launching drop wizard HTTP server. This class is responsible for configuring
 * any Jersey plugins, registering resources (controllers) and injecting those resources with
 * configuration properties from 'AppConfig'.
 */
@Slf4j
public class SpitfireServerApplication extends Application<SpitfireServerConfig> {

  private static final String[] DEFAULT_ARGS = new String[] {"server", "configuration.yml"};

  private ServerConfiguration<SpitfireServerConfig> serverConfiguration;

  /**
   * Main entry-point method, launches the drop-wizard http server. If no args are passed then will
   * use default values suitable for local development.
   */
  public static void main(final String[] args) throws Exception {
    final SpitfireServerApplication application = new SpitfireServerApplication();
    // if no args are provided then we will use default values.
    application.run(args.length == 0 ? DEFAULT_ARGS : args);
  }

  @Override
  public void initialize(final Bootstrap<SpitfireServerConfig> bootstrap) {
    serverConfiguration =
        ServerConfiguration.build(
                bootstrap,
                new WebsocketConfig(GameConnectionWebSocket.class, WebsocketPaths.GAME_CONNECTIONS),
                new WebsocketConfig(
                    PlayerConnectionWebSocket.class, WebsocketPaths.PLAYER_CONNECTIONS))
            .enableEnvironmentVariablesInConfig()
            .enableBetterJdbiExceptions();
  }

  @Override
  public void run(final SpitfireServerConfig configuration, final Environment environment) {
    final Jdbi jdbi =
        new JdbiFactory()
            .build(environment, configuration.getDatabase(), "postgresql-connection-pool");

    MapsModuleRowMappers.rowMappers().forEach(jdbi::registerRowMapper);
    LobbyModuleRowMappers.rowMappers().forEach(jdbi::registerRowMapper);

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

    serverConfiguration.registerRequestFilter(
        environment, BannedPlayerFilter.newBannedPlayerFilter(jdbi));

    final MetricRegistry metrics = new MetricRegistry();
    AuthenticationConfiguration.enableAuthentication(
        environment,
        metrics,
        ApiKeyAuthenticator.build(jdbi),
        new RoleAuthorizer(),
        AuthenticatedUser.class);

    serverConfiguration.registerExceptionMappers(environment, List.of(new IllegalArgumentMapper()));

    final var sessionIsBannedCheck = SessionBannedCheck.build(jdbi);
    final var gameConnectionMessagingBus = new WebSocketMessagingBus();

    GenericWebSocket.init(
        GameConnectionWebSocket.class, gameConnectionMessagingBus, sessionIsBannedCheck);

    final var playerConnectionMessagingBus = new WebSocketMessagingBus();
    GenericWebSocket.init(
        PlayerConnectionWebSocket.class, playerConnectionMessagingBus, sessionIsBannedCheck);

    final var chatters = Chatters.build();
    ChatMessagingService.build(chatters, jdbi).configure(playerConnectionMessagingBus);

    endPointControllers(
            configuration, jdbi, chatters, playerConnectionMessagingBus, gameConnectionMessagingBus)
        .forEach(controller -> environment.jersey().register(controller));
  }

  private List<Object> endPointControllers(
      final SpitfireServerConfig appConfig,
      final Jdbi jdbi,
      final Chatters chatters,
      final WebSocketMessagingBus playerMessagingBus,
      final WebSocketMessagingBus gameMessagingBus) {
    final GameListing gameListing = GameListing.build(jdbi, playerMessagingBus);
    return ImmutableList.of(
        // lobby module controllers
        AccessLogController.build(jdbi),
        BadWordsController.build(jdbi),
        CreateAccountController.build(jdbi),
        DisconnectUserController.build(jdbi, chatters, playerMessagingBus),
        ForgotPasswordController.build(appConfig, jdbi),
        GameChatHistoryController.build(jdbi),
        GameHostingController.build(jdbi),
        GameListingController.build(gameListing),
        LobbyWatcherController.build(appConfig, jdbi, gameListing),
        LoginController.build(jdbi, chatters),
        UsernameBanController.build(jdbi),
        UserBanController.build(jdbi, chatters, playerMessagingBus, gameMessagingBus),
        ErrorReportController.build(appConfig, jdbi),
        ModeratorAuditHistoryController.build(jdbi),
        ModeratorsController.build(jdbi),
        MuteUserController.build(chatters),
        PlayerInfoController.build(jdbi, chatters, gameListing),
        PlayersInGameController.build(gameListing),
        RemoteActionsController.build(jdbi, gameMessagingBus),
        UpdateAccountController.build(jdbi),

        // maps module controllers
        MapsController.build(jdbi),
        MapTagAdminController.build(jdbi));
  }
}
