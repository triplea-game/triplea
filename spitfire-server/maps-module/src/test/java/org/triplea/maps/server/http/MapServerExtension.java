package org.triplea.maps.server.http;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import java.util.Collection;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.triplea.dropwizard.test.DropwizardServerExtension;
import org.triplea.maps.server.db.RowMappers;

public class MapServerExtension extends DropwizardServerExtension<MapsConfig> {

  private static DropwizardTestSupport<MapsConfig> testSupport =
      new DropwizardTestSupport<>(MapsServer.class, "configuration.yml");

  @Override
  public DropwizardTestSupport<MapsConfig> getSupport() {
    return testSupport;
  }

  @Override
  protected DataSourceFactory getDatabase() {
    return testSupport.getConfiguration().getDatabase();
  }

  @Override
  protected Collection<RowMapperFactory> rowMappers() {
    return RowMappers.rowMappers();
  }
}
