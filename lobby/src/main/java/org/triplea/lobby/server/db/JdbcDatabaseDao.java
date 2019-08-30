package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import lombok.Getter;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@Getter(onMethod_ = {@Override})
class JdbcDatabaseDao implements DatabaseDao {

  private final AccessLogDao accessLogDao;
  private final BadWordDao badWordDao;
  private final UsernameBlacklistDao usernameBlacklistDao;
  private final UserBanDao bannedMacDao;
  private final UserDao userDao;
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  private final UserJdbiDao userJdbiDao;

  JdbcDatabaseDao(final Database database) {
    final Jdbi jdbi = JdbiDatabase.newConnection();
    moderatorAuditHistoryDao = jdbi.onDemand(ModeratorAuditHistoryDao.class);
    userJdbiDao = jdbi.onDemand(UserJdbiDao.class);

    final Supplier<Connection> connection = connectionSupplier(database);

    accessLogDao = new AccessLogController(connection);
    badWordDao = new BadWordController(connection);
    bannedMacDao = new BannedMacController(connection, moderatorAuditHistoryDao, userJdbiDao);
    usernameBlacklistDao =
        new UsernameBlacklistController(connection, moderatorAuditHistoryDao, userJdbiDao);
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
