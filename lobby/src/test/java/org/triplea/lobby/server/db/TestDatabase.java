package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class for creating DB connections in test. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestDatabase {
  private static final String CONNECTION_URL =
      String.format("jdbc:postgresql://%s:%d/%s", "localhost", 5432, "lobby_db");

  /** Creates a new DB connection to localhost. */
  public static Connection newConnection() throws SQLException {
    final Connection connection =
        DriverManager.getConnection(CONNECTION_URL, getConnectionProperties());
    connection.setAutoCommit(false);
    return connection;
  }

  private static Properties getConnectionProperties() {
    final Properties props = new Properties();
    props.put("user", "lobby_user");
    props.put("password", "postgres");
    return props;
  }
}
