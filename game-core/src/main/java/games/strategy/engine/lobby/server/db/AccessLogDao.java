package games.strategy.engine.lobby.server.db;

import java.sql.SQLException;

import games.strategy.engine.lobby.server.User;
import games.strategy.engine.lobby.server.login.UserType;

/**
 * Data access object for the access log table.
 */
public interface AccessLogDao {
  /**
   * Inserts a new record in the access log table.
   *
   * @param user The user who accessed the lobby.
   * @param userType The type of the user who accessed the lobby.
   *
   * @throws SQLException If an error occurs while logging the access.
   */
  void insert(User user, UserType userType) throws SQLException;
}
