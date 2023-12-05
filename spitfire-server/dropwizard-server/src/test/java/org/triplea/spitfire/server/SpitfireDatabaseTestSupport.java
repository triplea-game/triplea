package org.triplea.spitfire.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.triplea.db.LobbyModuleRowMappers;
import org.triplea.spitfire.database.DatabaseTestSupport;

public class SpitfireDatabaseTestSupport extends DatabaseTestSupport {

  @Override
  protected Collection<RowMapperFactory> rowMappers() {
    final List<RowMapperFactory> rowMappers = new ArrayList<>();
    rowMappers.addAll(LobbyModuleRowMappers.rowMappers());
    return rowMappers;
  }
}
