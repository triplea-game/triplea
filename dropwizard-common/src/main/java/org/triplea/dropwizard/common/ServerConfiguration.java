package org.triplea.dropwizard.common;

import es.moki.ratelimij.dropwizard.RateLimitBundle;
import es.moki.ratelimitj.inmemory.InMemoryRateLimiterFactory;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import java.util.Arrays;
import java.util.List;
import javax.websocket.server.ServerEndpointConfig;
import javax.ws.rs.container.ContainerRequestFilter;
import lombok.AllArgsConstructor;

/**
 * Facilitates configuration for a dropwizard server Application class.
 *
 * @param <T> Configuration class type of the server.
 */
public class ServerConfiguration<T extends Configuration> {

  private final Bootstrap<T> bootstrap;

  @AllArgsConstructor
  public static class WebsocketConfig {
    private final Class<?> websocketClass;
    private final String path;
  }

  public ServerConfiguration(
      final Bootstrap<T> bootstrap, final WebsocketConfig... websocketConfigs) {
    this.bootstrap = bootstrap;

    final ServerEndpointConfig[] websockets = addWebsockets(websocketConfigs);
    bootstrap.addBundle(new WebsocketBundle(websockets));
  }

  private ServerEndpointConfig[] addWebsockets(final WebsocketConfig... websocketConfigs) {
    return Arrays.stream(websocketConfigs)
        .map(
            websocketConfig ->
                ServerEndpointConfig.Builder.create(
                        websocketConfig.websocketClass, websocketConfig.path)
                    .build())
        .toArray(ServerEndpointConfig[]::new);
  }

  /**
   * This bootstrap will replace ${...} values in YML configuration with environment variable
   * values. Without it, all values in the YML configuration are treated as literals.
   */
  public void enableEnvironmentVariablesInConfig() {
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
  }

  /**
   * From: https://www.dropwizard.io/0.7.1/docs/manual/jdbi.html By adding the JdbiExceptionsBundle
   * to your application, Dropwizard will automatically unwrap ant thrown SQLException or
   * DBIException instances. This is critical for debugging, since otherwise only the common wrapper
   * exception’s stack trace is logged.
   */
  public void enableBetterJdbiExceptions() {
    bootstrap.addBundle(new JdbiExceptionsBundle());
  }

  /** Enables the rate4j annotations which can be used to do server endpoint throttling. */
  public void enableEndpointRateLimiting() {
    bootstrap.addBundle(new RateLimitBundle(new InMemoryRateLimiterFactory()));
  }

  public void registerRequestFilter(
      final Environment environment, final ContainerRequestFilter containerRequestFilter) {
    environment.jersey().register(containerRequestFilter);
  }

  /**
   * Registers an exception mapping, meaning an uncaught exception matching an exception mapper will
   * then "go through" the exception mapper. This can be used for example to register an exception
   * mapper for something like <code>IllegalArgumentException</code> to return a status 400 response
   * rather than a status 500 response. Exception mappers can be also be used for common logging or
   * for returning a specific response entity.
   */
  public void registerExceptionMappers(
      final Environment environment, final List<Object> exceptionMappers) {
    exceptionMappers.forEach(mapper -> environment.jersey().register(mapper));
  }
}
