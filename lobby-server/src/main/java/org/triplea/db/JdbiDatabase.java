package org.triplea.db;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.triplea.db.dao.access.log.AccessLogRecord;
import org.triplea.db.dao.api.key.PlayerApiKeyLookupRecord;
import org.triplea.db.dao.api.key.PlayerIdentifiersByApiKeyLookup;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryRecord;
import org.triplea.db.dao.moderator.ModeratorUserDaoData;
import org.triplea.db.dao.moderator.chat.history.ChatHistoryRecord;
import org.triplea.db.dao.moderator.player.info.PlayerAliasRecord;
import org.triplea.db.dao.moderator.player.info.PlayerBanRecord;
import org.triplea.db.dao.user.ban.BanLookupRecord;
import org.triplea.db.dao.user.ban.UserBanRecord;
import org.triplea.db.dao.user.history.PlayerHistoryRecord;
import org.triplea.db.dao.user.role.UserRoleLookup;
import org.triplea.db.dao.username.ban.UsernameBanRecord;

/** Utility to get connections to the Postgres lobby database. */
@Log
@UtilityClass
public final class JdbiDatabase {
  /**
   * Returns all row mappers. These are classes that map result set values to corresponding return
   * objects.
   */
  public static List<RowMapperFactory> rowMappers() {
    return List.of(
        ConstructorMapper.factory(AccessLogRecord.class),
        ConstructorMapper.factory(BanLookupRecord.class),
        ConstructorMapper.factory(ChatHistoryRecord.class),
        ConstructorMapper.factory(ModeratorAuditHistoryRecord.class),
        ConstructorMapper.factory(ModeratorUserDaoData.class),
        ConstructorMapper.factory(PlayerAliasRecord.class),
        ConstructorMapper.factory(PlayerApiKeyLookupRecord.class),
        ConstructorMapper.factory(PlayerBanRecord.class),
        ConstructorMapper.factory(PlayerHistoryRecord.class),
        ConstructorMapper.factory(PlayerIdentifiersByApiKeyLookup.class),
        ConstructorMapper.factory(UserBanRecord.class),
        ConstructorMapper.factory(UsernameBanRecord.class),
        ConstructorMapper.factory(UserRoleLookup.class));
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
