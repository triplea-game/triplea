package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.annotation.Nonnull;
import lombok.Builder;

/** Utility to get connections to the Postgres lobby database. */
@Builder
public final class Database {

  @Nonnull private final String postgresHost;
  @Nonnull private final Integer postgresPort;
  @Nonnull private final String postgresDatabase;
  @Nonnull private final String postgresUser;
  @Nonnull private final String postgresPassword;

  Connection newConnection() throws SQLException {
    final Connection connection =
        DriverManager.getConnection(getConnectionUrl(), getConnectionProperties());
    connection.setAutoCommit(false);
    return connection;
  }

  private String getConnectionUrl() {
    return String.format(
        "jdbc:postgresql://%s:%d/%s", postgresHost, postgresPort, postgresDatabase);
  }

  private Properties getConnectionProperties() {
    final Properties props = new Properties();
    props.put("user", postgresUser);
    props.put("password", postgresPassword);
    return props;
  }

  public DatabaseDao newDatabaseDao() {
    return new JdbcDatabaseDao(this);
  }
}
