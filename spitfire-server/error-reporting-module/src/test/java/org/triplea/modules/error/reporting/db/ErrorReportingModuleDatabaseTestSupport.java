package org.triplea.modules.error.reporting.db;

import java.util.Collection;
import java.util.List;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.triplea.spitfire.database.DatabaseTestSupport;

public class ErrorReportingModuleDatabaseTestSupport extends DatabaseTestSupport {
  @Override
  protected Collection<RowMapperFactory> rowMappers() {
    return List.of();
  }
}
