package org.triplea.lobby.server.db;

import java.sql.SQLException;
import java.util.logging.Level;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.triplea.lobby.server.db.data.AccessLogDaoData;
import org.triplea.lobby.server.db.data.ApiKeyDaoData;
import org.triplea.lobby.server.db.data.ModeratorAuditHistoryDaoData;
import org.triplea.lobby.server.db.data.ModeratorUserDaoData;
import org.triplea.lobby.server.db.data.UserBanDaoData;
import org.triplea.lobby.server.db.data.UsernameBanDaoData;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

/**
 * Utility to get connections to the Postgres lobby database.
 */
@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JdbiDatabase {

  /**
   * Creates a new connection to database. This connection should
   * only be used by the TripleA Java Lobby. DropWizard will create
   * a connection from configuration automatically.
   */
  public static Jdbi newConnection() {
    final Jdbi jdbi = Jdbi.create(
        String.format(
            "jdbc:postgresql://%s:%s/%s",
            DatabaseEnvironmentVariable.POSTGRES_HOST.getValue(),
            DatabaseEnvironmentVariable.POSTGRES_PORT.getValue(),
            DatabaseEnvironmentVariable.POSTGRES_DATABASE.getValue()),
        DatabaseEnvironmentVariable.POSTGRES_USER.getValue(),
        DatabaseEnvironmentVariable.POSTGRES_PASSWORD.getValue());
    jdbi.installPlugin(new SqlObjectPlugin());
    registerRowMappers(jdbi);
    registerSqlLogger(jdbi);
    return jdbi;
  }

  /**
   * Registers all JDBI row mappers. These are classes that map result set values to corresponding return objects.
   */
  public static void registerRowMappers(final Jdbi jdbi) {
    jdbi.registerRowMapper(AccessLogDaoData.class, AccessLogDaoData.buildResultMapper());
    jdbi.registerRowMapper(ApiKeyDaoData.class, ApiKeyDaoData.buildResultMapper());
    jdbi.registerRowMapper(UserBanDaoData.class, UserBanDaoData.buildResultMapper());
    jdbi.registerRowMapper(UsernameBanDaoData.class, UsernameBanDaoData.buildResultMapper());
    jdbi.registerRowMapper(ModeratorAuditHistoryDaoData.class, ModeratorAuditHistoryDaoData.buildResultMapper());
    jdbi.registerRowMapper(ModeratorUserDaoData.class, ModeratorUserDaoData.buildResultMapper());
  }

  /**
   * Adds a logger to JDBI that will log SQL statements before they are executed.
   */
  public static void registerSqlLogger(final Jdbi jdbi) {
    jdbi.setSqlLogger(new SqlLogger() {
      @Override
      public void logBeforeExecution(final StatementContext context) {
        log.info("Executing SQL: " + context.getRawSql());
      }

      @Override
      public void logAfterExecution(final StatementContext context) {

      }

      @Override
      public void logException(final StatementContext context, final SQLException ex) {
        log.log(Level.SEVERE, "Exception executing SQL: " + context.getRawSql(), ex);
      }
    });
  }
}
