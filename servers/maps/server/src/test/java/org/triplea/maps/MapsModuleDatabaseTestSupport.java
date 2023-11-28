package org.triplea.maps;

import java.util.Collection;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.triplea.spitfire.database.DatabaseTestSupport;

public class MapsModuleDatabaseTestSupport extends DatabaseTestSupport {
  @Override
  protected Collection<RowMapperFactory> rowMappers() {
    return MapsModuleRowMappers.rowMappers();
  }
}
