package org.triplea.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Superclass for all DAO implementations.
 */
abstract class AbstractController {
  private final Database database;

  AbstractController(final Database database) {
    checkNotNull(database);

    this.database = database;
  }

  final Connection newDatabaseConnection() throws SQLException {
    return database.newConnection();
  }
}
