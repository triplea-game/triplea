package org.triplea.http;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import io.dropwizard.Application;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.List;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.JdbiDatabase;
import org.triplea.dropwizard.common.AuthenticationConfiguration;
import org.triplea.dropwizard.common.ServerConfiguration;
import org.triplea.dropwizard.common.ServerConfiguration.WebsocketConfig;
import org.triplea.http.client.web.socket.WebsocketPaths;
import org.triplea.modules.access.authentication.ApiKeyAuthenticator;
import org.triplea.modules.access.authentication.AuthenticatedUser;
import org.triplea.modules.access.authorization.BannedPlayerFilter;
import org.triplea.modules.access.authorization.RoleAuthorizer;
import org.triplea.modules.chat.ChatMessagingService;
import org.triplea.modules.chat.Chatters;
import org.triplea.modules.error.reporting.ErrorReportController;
import org.triplea.modules.forgot.password.ForgotPasswordController;
import org.triplea.modules.game.hosting.GameHostingController;
import org.triplea.modules.game.listing.GameListing;
import org.triplea.modules.game.listing.GameListingController;
import org.triplea.modules.game.lobby.watcher.LobbyWatcherController;
import org.triplea.modules.game.participants.PlayersInGameController;
import org.triplea.modules.moderation.access.log.AccessLogController;
import org.triplea.modules.moderation.audit.history.ModeratorAuditHistoryController;
import org.triplea.modules.moderation.bad.words.BadWordsController;
import org.triplea.modules.moderation.ban.name.UsernameBanController;
import org.triplea.modules.moderation.ban.user.UserBanController;
import org.triplea.modules.moderation.chat.history.GameChatHistoryController;
import org.triplea.modules.moderation.disconnect.user.DisconnectUserController;
import org.triplea.modules.moderation.moderators.ModeratorsController;
import org.triplea.modules.moderation.mute.user.MuteUserController;
import org.triplea.modules.moderation.remote.actions.RemoteActionsController;
import org.triplea.modules.player.info.PlayerInfoController;
import org.triplea.modules.user.account.create.CreateAccountController;
import org.triplea.modules.user.account.login.LoginController;
import org.triplea.modules.user.account.update.UpdateAccountController;
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
public class LobbyServer extends Application<LobbyServerConfig> {

  private static final String[] DEFAULT_ARGS = new String[] {"server", "configuration.yml"};

  private ServerConfiguration<LobbyServerConfig> serverConfiguration;

  /**
   * Main entry-point method, launches the drop-wizard http server. If no args are passed then will
   * use default values suitable for local development.
   */
  public static void main(final String[] args) throws Exception {
    final LobbyServer application = new LobbyServer();
    // if no args are provided then we will use default values.
    application.run(args.length == 0 ? DEFAULT_ARGS : args);
  }

  @Override
  public void initialize(final Bootstrap<LobbyServerConfig> bootstrap) {
    serverConfiguration =
        new ServerConfiguration<>(
            bootstrap,
            new WebsocketConfig(GameConnectionWebSocket.class, WebsocketPaths.GAME_CONNECTIONS),
            new WebsocketConfig(
                PlayerConnectionWebSocket.class, WebsocketPaths.PLAYER_CONNECTIONS));

    serverConfiguration.enableEnvironmentVariablesInConfig();
    serverConfiguration.enableBetterJdbiExceptions();
  }

  @Override
  public void run(final LobbyServerConfig configuration, final Environment environment) {
    if (configuration.isLogRequestAndResponses()) {
      serverConfiguration.enableRequestResponseLogging(environment);
    }

    final Jdbi jdbi =
        new JdbiFactory()
            .build(environment, configuration.getDatabase(), "postgresql-connection-pool");

    JdbiDatabase.rowMappers().forEach(jdbi::registerRowMapper);

    if (configuration.isLogSqlStatements()) {
      JdbiDatabase.registerSqlLogger(jdbi);
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
      final LobbyServerConfig appConfig,
      final Jdbi jdbi,
      final Chatters chatters,
      final WebSocketMessagingBus playerMessagingBus,
      final WebSocketMessagingBus gameMessagingBus) {
    final GameListing gameListing = GameListing.build(jdbi, playerMessagingBus);
    return ImmutableList.of(
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
        UpdateAccountController.build(jdbi));
  }
}
