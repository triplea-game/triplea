package org.triplea.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import lombok.extern.java.Log;

/**
 * Superclass for all DAO implementations.
 */
@Log
abstract class AbstractController {
  private final Database database;

  AbstractController(final Database database) {
    checkNotNull(database);

    this.database = database;
  }

  final Connection newDatabaseConnection() throws SQLException {
    return database.newConnection();
  }

  static RuntimeException newDatabaseException(final String message, final SQLException e) {
    log.log(Level.SEVERE, message, e);
    return new IllegalStateException(String.format("%s (%s)", message, e.getMessage()));
  }
}
