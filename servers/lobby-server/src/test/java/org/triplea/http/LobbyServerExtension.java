package org.triplea.http;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import java.util.Collection;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.triplea.db.JdbiDatabase;
import org.triplea.dropwizard.test.DropwizardServerExtension;

public class LobbyServerExtension extends DropwizardServerExtension<LobbyServerConfig> {
  private static DropwizardTestSupport<LobbyServerConfig> testSupport =
      new DropwizardTestSupport<>(LobbyServer.class, "configuration.yml");

  @Override
  protected DropwizardTestSupport<LobbyServerConfig> getSupport() {
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
