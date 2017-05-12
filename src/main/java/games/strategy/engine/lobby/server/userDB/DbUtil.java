package games.strategy.engine.lobby.server.userDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

class DbUtil {
  private static final Logger s_logger = Logger.getLogger(DbUtil.class.getName());

  static void closeConnection(final Connection con) {
    try {
      con.close();
    } catch (final SQLException e) {
      s_logger.log(Level.WARNING, "Error closing connection", e);
    }
  }
}
