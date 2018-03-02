package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import games.strategy.engine.config.lobby.LobbyPropertyReader;
import games.strategy.engine.lobby.server.LobbyContext;

/**
 * Utility to get connections to the database.
 */
public final class Database {
  private static final Properties connectionProperties = getConnectionProperties();

  private Database() {}

  private static Properties getConnectionProperties() {
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
      final Connection connection = DriverManager.getConnection(getConnectionUrl(), connectionProperties);
      connection.setAutoCommit(false);
      return connection;
    } catch (final SQLException e) {
      throw new RuntimeException("Failure getting db connection", e);
    }
  }

  private static String getConnectionUrl() {
    final LobbyPropertyReader lobbyPropertyReader = LobbyContext.lobbyPropertyReader();
    return String.format(
        "jdbc:postgresql://%s:%d/%s",
        lobbyPropertyReader.getPostgresHost(),
        lobbyPropertyReader.getPostgresPort(),
        lobbyPropertyReader.getPostgresDatabase());
  }
}
