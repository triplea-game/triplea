package org.triplea.dropwizard.common;

import java.sql.SQLException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;

@UtilityClass
@Slf4j
public class JdbiLogging {
  /** Adds a logger to JDBI that will log SQL statements before they are executed. */
  public static void registerSqlLogger(final Jdbi jdbi) {
    jdbi.setSqlLogger(
        new SqlLogger() {
          @Override
          public void logBeforeExecution(final StatementContext context) {
            log.info("Executing SQL: " + context.getRawSql());
          }

          @Override
          public void logAfterExecution(final StatementContext context) {}

          @Override
          public void logException(final StatementContext context, final SQLException ex) {
            log.error("Exception executing SQL: " + context.getRawSql(), ex);
          }
        });
  }
}
