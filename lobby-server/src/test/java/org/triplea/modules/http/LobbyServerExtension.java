package org.triplea.modules.http;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import java.util.Collection;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.triplea.db.JdbiDatabase;
import org.triplea.dropwizard.test.DropwizardServerExtension;
import org.triplea.http.AppConfig;
import org.triplea.http.ServerApplication;

class LobbyServerExtension extends DropwizardServerExtension<AppConfig> {
  private static DropwizardTestSupport<AppConfig> testSupport =
      new DropwizardTestSupport<>(ServerApplication.class, "configuration.yml");

  @Override
  protected DropwizardTestSupport<AppConfig> getSupport() {
    return testSupport;
  }

  @Override
  protected DataSourceFactory getDatabase() {
    return testSupport.getConfiguration().getDatabase();
  }

  @Override
  protected Collection<RowMapperFactory> rowMappers() {
    return JdbiDatabase.rowMappers();
  }
}
