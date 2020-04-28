package org.triplea.http;

import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import es.moki.ratelimij.dropwizard.RateLimitBundle;
import es.moki.ratelimitj.inmemory.InMemoryRateLimiterFactory;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.JdbiDatabase;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.web.socket.WebsocketPaths;
import org.triplea.modules.access.authentication.ApiKeyAuthenticator;
import org.triplea.modules.access.authentication.AuthenticatedUser;
import org.triplea.modules.access.authorization.BannedPlayerFilter;
import org.triplea.modules.access.authorization.RoleAuthorizer;
import org.triplea.modules.chat.ChatMessagingService;
import org.triplea.modules.chat.Chatters;
import org.triplea.modules.error.reporting.ErrorReportController;
import org.triplea.modules.forgot.password.ForgotPasswordController;
import org.triplea.modules.game.ConnectivityController;
import org.triplea.modules.game.hosting.GameHostingController;
import org.triplea.modules.game.listing.GameListing;
import org.triplea.modules.game.listing.GameListingController;
import org.triplea.modules.game.lobby.watcher.LobbyWatcherController;
import org.triplea.modules.moderation.access.log.AccessLogController;
import org.triplea.modules.moderation.audit.history.ModeratorAuditHistoryController;
import org.triplea.modules.moderation.bad.words.BadWordsController;
import org.triplea.modules.moderation.ban.name.UsernameBanController;
import org.triplea.modules.moderation.ban.user.UserBanController;
import org.triplea.modules.moderation.disconnect.user.DisconnectUserController;
import org.triplea.modules.moderation.moderators.ModeratorsController;
import org.triplea.modules.moderation.remote.actions.RemoteActionsController;
import org.triplea.modules.user.account.create.CreateAccountController;
import org.triplea.modules.user.account.login.LoginController;
import org.triplea.modules.user.account.update.UpdateAccountController;
import org.triplea.web.socket.GameConnectionWebSocket;
import org.triplea.web.socket.PlayerConnectionWebSocket;
import org.triplea.web.socket.SessionBannedCheck;
import org.triplea.web.socket.WebSocketMessagingBus;

/**
 * Main entry-point for launching drop wizard HTTP server. This class is responsible for configuring
 * any Jersey plugins, registering resources (controllers) and injecting those resources with
 * configuration properties from 'AppConfig'.
 */
public class ServerApplication extends Application<AppConfig> {

  private static final String[] DEFAULT_ARGS = new String[] {"server", "configuration.yml"};
  private ServerEndpointConfig gameConnectionWebsocket;
  private ServerEndpointConfig playerConnectionWebsocket;

  /**
   * Main entry-point method, launches the drop-wizard http server. If no args are passed then will
   * use default values suitable for local development.
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
        new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

    // From: https://www.dropwizard.io/0.7.1/docs/manual/jdbi.html
    // By adding the JdbiExceptionsBundle to your application, Dropwizard will automatically unwrap
    // ant thrown SQLException or DBIException instances. This is critical for debugging, since
    // otherwise only the common wrapper exception’s stack trace is logged.
    bootstrap.addBundle(new JdbiExceptionsBundle());
    bootstrap.addBundle(new RateLimitBundle(new InMemoryRateLimiterFactory()));

    // Note, websocket endpoint is instantiated dynamically on every new connection and does
    // not allow for constructor injection. To inject objects, we use 'userProperties' of the
    // socket configuration that can then be retrieved from a websocket session.
    gameConnectionWebsocket =
        ServerEndpointConfig.Builder.create(
                GameConnectionWebSocket.class, WebsocketPaths.GAME_CONNECTIONS)
            .build();

    playerConnectionWebsocket =
        ServerEndpointConfig.Builder.create(
                PlayerConnectionWebSocket.class, WebsocketPaths.PLAYER_CONNECTIONS)
            .build();

    bootstrap.addBundle(new WebsocketBundle(gameConnectionWebsocket, playerConnectionWebsocket));
  }

  @Override
  public void run(final AppConfig configuration, final Environment environment) {
    if (configuration.isLogRequestAndResponses()) {
      enableRequestResponseLogging(environment);
    }

    final MetricRegistry metrics = new MetricRegistry();
    final Jdbi jdbi = createJdbi(configuration, environment);

    environment.jersey().register(BannedPlayerFilter.newBannedPlayerFilter(jdbi));
    environment.jersey().register(new RolesAllowedDynamicFeature());
    enableAuthentication(environment, metrics, jdbi);

    exceptionMappers().forEach(mapper -> environment.jersey().register(mapper));

    final var sessionIsBannedCheck = SessionBannedCheck.build(jdbi);
    final var gameConnectionMessagingBus = new WebSocketMessagingBus();
    setupWebSocket(gameConnectionWebsocket, gameConnectionMessagingBus, sessionIsBannedCheck);

    final var playerConnectionMessagingBus = new WebSocketMessagingBus();
    setupWebSocket(playerConnectionWebsocket, playerConnectionMessagingBus, sessionIsBannedCheck);

    final var chatters = Chatters.build(jdbi);
    ChatMessagingService.build(chatters, jdbi).configure(playerConnectionMessagingBus);

    endPointControllers(
            configuration, jdbi, chatters, playerConnectionMessagingBus, gameConnectionMessagingBus)
        .forEach(controller -> environment.jersey().register(controller));
  }

  private static void enableRequestResponseLogging(final Environment environment) {
    environment
        .jersey()
        .register(
            new LoggingFeature(
                Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME),
                Level.INFO,
                LoggingFeature.Verbosity.PAYLOAD_ANY,
                LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
  }

  private Jdbi createJdbi(final AppConfig configuration, final Environment environment) {
    final JdbiFactory factory = new JdbiFactory();
    final Jdbi jdbi =
        factory.build(environment, configuration.getDatabase(), "postgresql-connection-pool");
    JdbiDatabase.registerRowMappers(jdbi);

    if (configuration.isLogSqlStatements()) {
      JdbiDatabase.registerSqlLogger(jdbi);
    }
    return jdbi;
  }

  private static void enableAuthentication(
      final Environment environment, final MetricRegistry metrics, final Jdbi jdbi) {
    environment
        .jersey()
        .register(
            new AuthDynamicFeature(
                new OAuthCredentialAuthFilter.Builder<AuthenticatedUser>()
                    .setAuthenticator(buildAuthenticator(metrics, jdbi))
                    .setAuthorizer(new RoleAuthorizer())
                    .setPrefix(AuthenticationHeaders.KEY_BEARER_PREFIX)
                    .buildAuthFilter()));
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(AuthenticatedUser.class));
  }

  private static CachingAuthenticator<String, AuthenticatedUser> buildAuthenticator(
      final MetricRegistry metrics, final Jdbi jdbi) {
    return new CachingAuthenticator<>(
        metrics,
        new ApiKeyAuthenticator(ApiKeyDaoWrapper.build(jdbi)),
        CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(10)).maximumSize(10000));
  }

  private List<Object> exceptionMappers() {
    return ImmutableList.of(new IllegalArgumentMapper());
  }

  private static void setupWebSocket(
      final ServerEndpointConfig websocket,
      final WebSocketMessagingBus webSocketMessagingBus,
      final Predicate<Session> sessionBanCheck) {

    // Inject beans into websocket endpoints
    websocket
        .getUserProperties()
        .putAll(
            Map.of(
                WebSocketMessagingBus.MESSAGING_BUS_KEY, //
                webSocketMessagingBus,
                SessionBannedCheck.BAN_CHECK_KEY,
                sessionBanCheck));
  }

  private List<Object> endPointControllers(
      final AppConfig appConfig,
      final Jdbi jdbi,
      final Chatters chatters,
      final WebSocketMessagingBus playerMessagingBus,
      final WebSocketMessagingBus gameMessagingBus) {
    final GameListing gameListing = GameListing.build(jdbi, playerMessagingBus);
    return ImmutableList.of(
        AccessLogController.build(jdbi),
        BadWordsController.build(jdbi),
        ConnectivityController.build(),
        CreateAccountController.build(jdbi),
        DisconnectUserController.build(jdbi, chatters, playerMessagingBus),
        ForgotPasswordController.build(appConfig, jdbi),
        GameHostingController.build(jdbi),
        GameListingController.build(gameListing),
        LobbyWatcherController.build(jdbi, gameListing),
        LoginController.build(jdbi, chatters),
        UsernameBanController.build(jdbi),
        UserBanController.build(jdbi, chatters, playerMessagingBus, gameMessagingBus),
        ErrorReportController.build(appConfig, jdbi),
        ModeratorAuditHistoryController.build(jdbi),
        ModeratorsController.build(jdbi),
        RemoteActionsController.build(jdbi, gameMessagingBus),
        UpdateAccountController.build(jdbi));
  }
}
