package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

final class TestDatabase {

  private TestDatabase() {}

  static Connection newConnection() {
    try {
      final Connection connection =
          DriverManager.getConnection(getConnectionUrl(), getConnectionProperties());
      connection.setAutoCommit(false);
      return connection;
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getConnectionUrl() {
    return String.format(
        "jdbc:postgresql://%s:%d/%s",
        "localhost",
        5432,
        "ta_users");
  }

  private static Properties getConnectionProperties() {
    final Properties props = new Properties();
    props.put("user", "postgres");
    props.put("password", "postgres");
    return props;
  }
}
