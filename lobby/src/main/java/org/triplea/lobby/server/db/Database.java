package org.triplea.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.concurrent.ThreadSafe;

import org.triplea.lobby.server.config.LobbyConfiguration;

/**
 * Utility to get connections to the Postgres lobby database.
 *
 * <p>
 * Instances of this class are thread-safe if the underlying {@link LobbyConfiguration} is thread-safe.
 * </p>
 */
@ThreadSafe
public final class Database {
  private final LobbyConfiguration lobbyConfiguration;

  public Database(final LobbyConfiguration lobbyConfiguration) {
    checkNotNull(lobbyConfiguration);

    this.lobbyConfiguration = lobbyConfiguration;
  }

  public Connection newConnection() throws SQLException {
    final Connection connection = DriverManager.getConnection(getConnectionUrl(), getConnectionProperties());
    connection.setAutoCommit(false);
    return connection;
  }

  private String getConnectionUrl() {
    return String.format(
        "jdbc:postgresql://%s:%d/%s",
        lobbyConfiguration.getPostgresHost(),
        lobbyConfiguration.getPostgresPort(),
        lobbyConfiguration.getPostgresDatabase());
  }

  private Properties getConnectionProperties() {
    final Properties props = new Properties();
    props.put("user", lobbyConfiguration.getPostgresUser());
    props.put("password", lobbyConfiguration.getPostgresPassword());
    return props;
  }
}
