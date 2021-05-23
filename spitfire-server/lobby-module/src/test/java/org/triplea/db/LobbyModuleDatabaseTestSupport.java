package org.triplea.db;

import java.util.Collection;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.triplea.spitfire.database.DatabaseTestSupport;

public class LobbyModuleDatabaseTestSupport extends DatabaseTestSupport {
  @Override
  protected Collection<RowMapperFactory> rowMappers() {
    return LobbyModuleRowMappers.rowMappers();
  }
}
