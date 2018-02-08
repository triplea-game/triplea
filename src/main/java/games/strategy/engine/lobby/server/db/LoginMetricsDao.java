package games.strategy.engine.lobby.server.db;

import java.sql.SQLException;

import games.strategy.engine.lobby.server.login.LoginType;

/**
 * Data access object for the login metrics table.
 */
public interface LoginMetricsDao {
  /**
   * Records a successful login of the specified type in the login metrics table.
   *
   * @param loginType The type of login.
   *
   * @throws SQLException If an error occurs while adding the record.
   */
  void addSuccessfulLogin(LoginType loginType) throws SQLException;
}
