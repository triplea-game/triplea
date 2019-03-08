package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

import lombok.Getter;


@Getter(onMethod_ = {@Override})
class JdbcDatabaseDao implements DatabaseDao {

  private final BadWordDao badWordDao;
  private final BannedUsernameDao bannedUsernameDao;
  private final BannedMacDao bannedMacDao;
  private final MutedUsernameDao mutedUsernameDao;
  private final MutedMacDao mutedMacDao;
  private final UserDao userDao;

  JdbcDatabaseDao(final Database database) {
    final Supplier<Connection> connection = connectionSupplier(database);

    badWordDao = new BadWordController(connection);
    bannedMacDao = new BannedMacController(connection);
    bannedUsernameDao = new BannedUsernameController(connection);
    mutedUsernameDao = new MutedUsernameController(connection);
    mutedMacDao = new MutedMacController(connection);
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
