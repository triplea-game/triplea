package org.triplea.server.http;

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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.server.ServerEndpointConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.lobby.chat.LobbyChatClient;
import org.triplea.http.client.remote.actions.RemoteActionListeners;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.lobby.server.db.dao.api.key.LobbyApiKeyDaoWrapper;
import org.triplea.server.access.ApiKeyAuthenticator;
import org.triplea.server.access.AuthenticatedUser;
import org.triplea.server.access.BannedPlayerFilter;
import org.triplea.server.access.RoleAuthorizer;
import org.triplea.server.error.reporting.ErrorReportControllerFactory;
import org.triplea.server.forgot.password.ForgotPasswordControllerFactory;
import org.triplea.server.lobby.chat.ChatWebsocket;
import org.triplea.server.lobby.chat.MessagingServiceFactory;
import org.triplea.server.lobby.chat.event.processing.Chatters;
import org.triplea.server.lobby.chat.moderation.ModeratorChatControllerFactory;
import org.triplea.server.lobby.game.ConnectivityControllerFactory;
import org.triplea.server.lobby.game.hosting.GameHostingControllerFactory;
import org.triplea.server.lobby.game.listing.GameListingControllerFactory;
import org.triplea.server.lobby.game.listing.GameListingFactory;
import org.triplea.server.lobby.game.listing.LobbyWatcherControllerFactory;
import org.triplea.server.moderator.toolbox.access.log.AccessLogControllerFactory;
import org.triplea.server.moderator.toolbox.audit.history.ModeratorAuditHistoryControllerFactory;
import org.triplea.server.moderator.toolbox.bad.words.BadWordControllerFactory;
import org.triplea.server.moderator.toolbox.banned.names.UsernameBanControllerFactory;
import org.triplea.server.moderator.toolbox.banned.users.UserBanControllerFactory;
import org.triplea.server.moderator.toolbox.moderators.ModeratorsControllerFactory;
import org.triplea.server.remote.actions.RemoteActionsControllerFactory;
import org.triplea.server.remote.actions.RemoteActionsEventQueue;
import org.triplea.server.remote.actions.RemoteActionsEventQueueFactory;
import org.triplea.server.remote.actions.RemoteActionsWebSocket;
import org.triplea.server.user.account.create.CreateAccountControllerFactory;
import org.triplea.server.user.account.login.LoginControllerFactory;
import org.triplea.server.user.account.update.UpdateAccountControllerFactory;

/**
 * Main entry-point for launching drop wizard HTTP server. This class is responsible for configuring
 * any Jersey plugins, registering resources (controllers) and injecting those resources with
 * configuration properties from 'AppConfig'.
 */
public class ServerApplication extends Application<AppConfig> {

  private static final String[] DEFAULT_ARGS = new String[] {"server", "configuration.yml"};
  private ServerEndpointConfig chatSocketConfiguration;
  private ServerEndpointConfig remoteActionsConfiguration;

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
    chatSocketConfiguration =
        ServerEndpointConfig.Builder.create(
                ChatWebsocket.class, LobbyChatClient.LOBBY_CHAT_WEBSOCKET_PATH)
            .build();

    remoteActionsConfiguration =
        ServerEndpointConfig.Builder.create(
                RemoteActionsWebSocket.class, RemoteActionListeners.NOTIFICATIONS_WEBSOCKET_PATH)
            .build();
    bootstrap.addBundle(new WebsocketBundle(chatSocketConfiguration, remoteActionsConfiguration));
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

    final var chatters = new Chatters();
    final var remoteActionsEventQueue = RemoteActionsEventQueueFactory.newRemoteActionsEventQueue();

    endPointControllers(configuration, jdbi, chatters, remoteActionsEventQueue)
        .forEach(controller -> environment.jersey().register(controller));

    // Inject beans into websocket endpoint
    chatSocketConfiguration
        .getUserProperties()
        .put(ChatWebsocket.MESSAGING_SERVICE_KEY, MessagingServiceFactory.build(jdbi, chatters));

    remoteActionsConfiguration
        .getUserProperties()
        .put(RemoteActionsWebSocket.ACTIONS_QUEUE_KEY, remoteActionsEventQueue);
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
        new ApiKeyAuthenticator(new LobbyApiKeyDaoWrapper(jdbi)),
        CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).maximumSize(10000));
  }

  private List<Object> exceptionMappers() {
    return ImmutableList.of(new IllegalArgumentMapper());
  }

  private List<Object> endPointControllers(
      final AppConfig appConfig,
      final Jdbi jdbi,
      final Chatters chatters,
      final RemoteActionsEventQueue remoteActionsEventQueue) {
    final var gameListing = GameListingFactory.buildGameListing(jdbi);

    return ImmutableList.of(
        AccessLogControllerFactory.buildController(jdbi),
        BadWordControllerFactory.buildController(jdbi),
        ConnectivityControllerFactory.buildController(),
        CreateAccountControllerFactory.buildController(jdbi),
        ForgotPasswordControllerFactory.buildController(appConfig, jdbi),
        GameHostingControllerFactory.buildController(jdbi),
        GameListingControllerFactory.buildController(gameListing),
        LobbyWatcherControllerFactory.buildController(gameListing),
        LoginControllerFactory.buildController(jdbi, chatters),
        ModeratorChatControllerFactory.buildController(jdbi, chatters, remoteActionsEventQueue),
        UsernameBanControllerFactory.buildController(appConfig, jdbi),
        UserBanControllerFactory.buildController(jdbi, chatters, remoteActionsEventQueue),
        ErrorReportControllerFactory.buildController(appConfig, jdbi),
        ModeratorAuditHistoryControllerFactory.buildController(appConfig, jdbi),
        ModeratorsControllerFactory.buildController(appConfig, jdbi),
        RemoteActionsControllerFactory.buildController(jdbi, remoteActionsEventQueue),
        UpdateAccountControllerFactory.buildController(jdbi));
  }
}
