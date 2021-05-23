package org.triplea.spitfire.server;

import com.google.common.base.Preconditions;
import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import java.net.URI;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Extension to start a dropwizard server. This extension can read the 'configuration.yml' of the
 * server which allows access to database connectivity parameters. Tests can have several objects
 * injected into them by declaring those objects as constructor or test method parameters. Those
 * objects are:
 *
 * <ul>
 *   <li>URI - server URI of the running test server
 *   <li>JDBI - jdbi instance
 *   <li>JDBI on-demand class - any class (DAO classes) that can be instantiated via
 *       Jdbi.onDemand(Class)
 *   <li>Server Configuration - the configuration class of the dropwizard server
 * </ul>
 *
 * @param <C> Server configuration type.
 */
public abstract class DropwizardServerExtension<C extends Configuration>
    implements BeforeAllCallback, ParameterResolver {

  private static URI serverUri;

  /**
   * Implementations should return a *static* instance of DropwizardTestSupport. If returning a
   * local instance, the test server may be turned off after a single test class is done executing
   * and subsequent tests could fail due to server not being on. Example implementation
   *
   * <pre>{@code
   * @Getter
   * static DropwizardTestSupport<MapsConfig> testSupport =
   *   new DropwizardTestSupport<>(MapsServer.class, "configuration.yml")
   * }</pre>
   */
  protected abstract DropwizardTestSupport<C> getSupport();

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {
    final DropwizardTestSupport<C> support = getSupport();
    support.before();

    final String localUri = "http://localhost:" + support.getLocalPort();
    serverUri = URI.create(localUri);
  }

  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(URI.class)
        || parameterContext
            .getParameter()
            .getType()
            .equals(getSupport().getConfiguration().getClass());
  }

  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(URI.class)) {
      return Preconditions.checkNotNull(serverUri);
    } else {
      return getSupport().getConfiguration();
    }
  }
}
