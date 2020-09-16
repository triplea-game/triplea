package org.triplea.dropwizard.test;

import com.google.common.base.Preconditions;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Extension to start a dropwizard server. This extension can read the 'configuration.yml' of the
 * server which allows access to database connectivity parameters. Tests extended with this class
 * will have "URI" objects injected (taking the value of the server URI that has been started) and
 * any potential JDBI created DAO objects will also be injected as well.
 *
 * @param <C> Server configuration type.
 */
@Slf4j
public abstract class DropwizardServerExtension<C extends Configuration>
    implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

  private static Jdbi jdbi;
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

  /**
   * Returns datasource typically obtained from configuration. Typical implementation: {@code
   * getSupport().getConfiguration().getDatabase()}.
   */
  protected abstract DataSourceFactory getDatabase();

  /** Return all row mappers that should be registered with JDBI. */
  protected abstract Collection<RowMapperFactory> rowMappers();

  @Override
  public void beforeAll(final ExtensionContext context) {
    final DropwizardTestSupport<C> support = getSupport();
    log.info("Starting local server for testing..");
    support.before();

    if (jdbi == null) {
      jdbi =
          Jdbi.create(getDatabase().getUrl(), getDatabase().getUser(), getDatabase().getPassword());
      jdbi.installPlugin(new SqlObjectPlugin());
      rowMappers().forEach(jdbi::registerRowMapper);
      log.info("Created JDBI connection to: {}", getDatabase().getUrl());
    }

    final String localUri = "http://localhost:" + support.getLocalPort();
    serverUri = URI.create(localUri);
    log.info("Local server URL set to: {}", localUri);
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    final URL cleanupFileUrl = getClass().getClassLoader().getResource("db-cleanup.sql");
    if (cleanupFileUrl != null) {
      final String cleanupSql = Files.readString(Path.of(cleanupFileUrl.toURI()));
      jdbi.withHandle(handle -> handle.execute(cleanupSql));
    }
  }

  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    try {
      jdbi.onDemand(parameterContext.getParameter().getType());
      return true;
    } catch (final IllegalArgumentException ignored) {
      // ignore
    }
    return parameterContext.getParameter().getType().equals(URI.class);
  }

  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(URI.class)) {
      return Preconditions.checkNotNull(serverUri);
    }

    return jdbi.onDemand(parameterContext.getParameter().getType());
  }
}
