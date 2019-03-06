package org.triplea.lobby.server.db;

import java.sql.SQLException;
import java.util.logging.Level;

import lombok.extern.java.Log;

/**
 * Thrown when there is an unexpected exception interacting with database.
 */
@Log
class DatabaseException extends RuntimeException {
  private static final long serialVersionUID = -557834581550721114L;

  DatabaseException(final String message, final SQLException e) {
    super(message, e);
    log.log(Level.SEVERE, message, e);
  }

}
