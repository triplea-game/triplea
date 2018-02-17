package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import games.strategy.engine.lobby.server.LobbyContext;

/**
 * Utility to get connections to the database.
 */
public class Database {
  private static final Properties connectionProperties = getPostgresDbProps();

  private static Properties getPostgresDbProps() {
    final Properties props = new Properties();
    props.put("user", LobbyContext.lobbyPropertyReader().getPostgresUser());
    props.put("password", LobbyContext.lobbyPropertyReader().getPostgresPassword());
    return props;
  }

  /**
   * Creates and returns a new database connection.
   */
  public static Connection getPostgresConnection() {
    try {
      final Connection connection =
          DriverManager.getConnection("jdbc:postgresql://localhost/ta_users", connectionProperties);
      connection.setAutoCommit(false);
      return connection;
    } catch (final SQLException e) {
      throw new RuntimeException("Failure getting db connection", e);
    }
  }

}
