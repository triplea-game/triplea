package games.strategy.engine.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.concurrent.ThreadSafe;

import games.strategy.engine.config.lobby.LobbyPropertyReader;

/**
 * Utility to get connections to the Postgres lobby database.
 *
 * <p>
 * Instances of this class are thread-safe if the underlying {@link LobbyPropertyReader} is thread-safe.
 * </p>
 */
@ThreadSafe
public final class Database {
  private final LobbyPropertyReader lobbyPropertyReader;

  public Database(final LobbyPropertyReader lobbyPropertyReader) {
    checkNotNull(lobbyPropertyReader);

    this.lobbyPropertyReader = lobbyPropertyReader;
  }

  public Connection newConnection() throws SQLException {
    final Connection connection = DriverManager.getConnection(getConnectionUrl(), getConnectionProperties());
    connection.setAutoCommit(false);
    return connection;
  }

  private String getConnectionUrl() {
    return String.format(
        "jdbc:postgresql://%s:%d/%s",
        lobbyPropertyReader.getPostgresHost(),
        lobbyPropertyReader.getPostgresPort(),
        lobbyPropertyReader.getPostgresDatabase());
  }

  private Properties getConnectionProperties() {
    final Properties props = new Properties();
    props.put("user", lobbyPropertyReader.getPostgresUser());
    props.put("password", lobbyPropertyReader.getPostgresPassword());
    return props;
  }
}
