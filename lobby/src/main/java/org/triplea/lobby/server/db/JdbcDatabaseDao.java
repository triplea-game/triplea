package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UserLookupDao;

import lombok.Getter;


@Getter(onMethod_ = {@Override})
class JdbcDatabaseDao implements DatabaseDao {

  private final AccessLogDao accessLogDao;
  private final BadWordDao badWordDao;
  private final UsernameBlacklistDao usernameBlacklistDao;
  private final BannedMacDao bannedMacDao;
  private final UserDao userDao;
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  private final UserLookupDao userLookupDao;

  JdbcDatabaseDao(final Database database) {
    final Jdbi jdbi = JdbiDatabase.newConnection();
    moderatorAuditHistoryDao = jdbi.onDemand(ModeratorAuditHistoryDao.class);
    userLookupDao = jdbi.onDemand(UserLookupDao.class);

    final Supplier<Connection> connection = connectionSupplier(database);

    accessLogDao = new AccessLogController(connection);
    badWordDao = new BadWordController(connection);
    bannedMacDao = new BannedMacController(connection, moderatorAuditHistoryDao, userLookupDao);
    usernameBlacklistDao = new UsernameBlacklistController(connection, moderatorAuditHistoryDao, userLookupDao);
    userDao = new UserController(connection);
  }

  private static Supplier<Connection> connectionSupplier(final Database database) {
    return () -> {
      try {
        return database.newConnection();
      } catch (final SQLException e) {
        throw new RuntimeException("Failed creating database connection", e);
      }
    };
  }
}
