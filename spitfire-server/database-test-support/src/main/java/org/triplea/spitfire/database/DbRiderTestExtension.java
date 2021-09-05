package org.triplea.spitfire.database;

import com.github.database.rider.junit5.DBUnitExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Extend this class to more easily test database with DbRider. Once extended, a test can then use
 * DbRider '@DataSet' annotations to inject data into database. After each test executes, the
 * database is wiped clean.
 *
 * <p>Second, this extension will constructor or test method parameter inject any classes that can
 * be instantiated via a 'Jdbi.onDemand(class)' or 'new DaoClass(jdbi)'.
 */
@ExtendWith(DBUnitExtension.class)
public abstract class DbRiderTestExtension
    implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

  private static Jdbi jdbi;

  protected abstract String getDatabaseUser();

  protected abstract String getDatabasePassword();

  protected abstract String getDatabaseUrl();

  /** Return all row mappers that should be registered with JDBI. */
  protected abstract Collection<RowMapperFactory> rowMappers();

  @Override
  public void beforeAll(final ExtensionContext context) {
    if (jdbi == null) {
      jdbi = Jdbi.create(getDatabaseUrl(), getDatabaseUser(), getDatabasePassword());
      jdbi.installPlugin(new SqlObjectPlugin());
      rowMappers().forEach(jdbi::registerRowMapper);
    }
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

    // check if there is a one-arg (JDBI) constructor
    try {
      parameterContext.getParameter().getType().getConstructor(Jdbi.class);
      return true;
    } catch (final NoSuchMethodException e) {
      // no-op, object is constructed potentially another way
    }
    try {
      jdbi.onDemand(parameterContext.getParameter().getType());
      return true;
    } catch (final IllegalArgumentException ignored) {
      return false;
    }
  }

  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {

    // try to create the class using constructor that accepts one Jdbi
    try {
      final Constructor<?> constructor =
          parameterContext.getParameter().getType().getConstructor(Jdbi.class);
      return constructor.newInstance(jdbi);
    } catch (final NoSuchMethodException
        | InvocationTargetException
        | IllegalAccessException
        | InstantiationException e) {
      // no-op, object is constructed via 'jdbi.onDemand'
    }

    return jdbi.onDemand(parameterContext.getParameter().getType());
  }
}
