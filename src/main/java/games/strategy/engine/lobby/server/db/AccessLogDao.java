package games.strategy.engine.lobby.server.db;

import java.sql.SQLException;
import java.time.Instant;

import games.strategy.engine.lobby.server.User;

/**
 * Data access object for the access log table.
 */
public interface AccessLogDao {
  /**
   * Inserts a new record in the access log table.
   *
   * @param instant The instant of the access.
   * @param user The user who accessed the lobby.
   * @param authenticated {@code true} if the access was authenticated; otherwise {@code false}.
   *
   * @throws SQLException If an error occurs while logging the access.
   */
  void insert(Instant instant, User user, boolean authenticated) throws SQLException;
}
