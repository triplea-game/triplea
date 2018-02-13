package games.strategy.engine.lobby.server.db;

import java.sql.SQLException;
import java.time.Instant;

import games.strategy.engine.lobby.server.User;
import games.strategy.engine.lobby.server.login.AuthenticationType;

/**
 * Data access object for the access log table.
 */
public interface AccessLogDao {
  /**
   * Inserts a new record in the access log table.
   *
   * @param instant The instant of the access.
   * @param user The user who accessed the lobby.
   * @param authenticationType The type of authentication used to grant access to the lobby.
   *
   * @throws SQLException If an error occurs while logging the access.
   */
  void insert(Instant instant, User user, AuthenticationType authenticationType) throws SQLException;
}
