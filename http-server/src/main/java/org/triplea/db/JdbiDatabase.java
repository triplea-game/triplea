package org.triplea.db;

import java.sql.SQLException;
import java.util.logging.Level;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.triplea.db.dao.access.log.AccessLogRecord;
import org.triplea.db.dao.api.key.ApiKeyLookupRecord;
import org.triplea.db.dao.api.key.GamePlayerLookup;
import org.triplea.db.dao.user.ban.BanLookupRecord;
import org.triplea.db.dao.user.ban.UserBanRecord;
import org.triplea.db.dao.username.ban.UsernameBanRecord;
import org.triplea.db.data.ModeratorAuditHistoryDaoData;
import org.triplea.db.data.ModeratorUserDaoData;
import org.triplea.db.data.UserRoleLookup;

/** Utility to get connections to the Postgres lobby database. */
@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JdbiDatabase {

  /**
   * Creates a new connection to database. This connection should only be used by the TripleA Java
   * Lobby. DropWizard will create a connection from configuration automatically.
   */
  public static Jdbi newConnection() {
    final Jdbi jdbi =
        Jdbi.create(
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
   * Registers all JDBI row mappers. These are classes that map result set values to corresponding
   * return objects.
   */
  public static void registerRowMappers(final Jdbi jdbi) {
    jdbi.registerRowMapper(AccessLogRecord.class, AccessLogRecord.buildResultMapper());
    jdbi.registerRowMapper(BanLookupRecord.class, BanLookupRecord.buildResultMapper());
    jdbi.registerRowMapper(GamePlayerLookup.class, GamePlayerLookup.buildResultMapper());
    jdbi.registerRowMapper(ApiKeyLookupRecord.class, ApiKeyLookupRecord.buildResultMapper());
    jdbi.registerRowMapper(UserBanRecord.class, UserBanRecord.buildResultMapper());
    jdbi.registerRowMapper(UsernameBanRecord.class, UsernameBanRecord.buildResultMapper());
    jdbi.registerRowMapper(UserRoleLookup.class, UserRoleLookup.buildResultMapper());
    jdbi.registerRowMapper(
        ModeratorAuditHistoryDaoData.class, ModeratorAuditHistoryDaoData.buildResultMapper());
    jdbi.registerRowMapper(ModeratorUserDaoData.class, ModeratorUserDaoData.buildResultMapper());
  }

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
            log.log(Level.SEVERE, "Exception executing SQL: " + context.getRawSql(), ex);
          }
        });
  }
}
