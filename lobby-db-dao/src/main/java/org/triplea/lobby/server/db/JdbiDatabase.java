package org.triplea.lobby.server.db;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility to get connections to the Postgres lobby database.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JdbiDatabase {
  /**
   * Creates a new connection to database.
   */
  public static Jdbi newConnection() {
    final Jdbi jdbi = Jdbi.create(
        String.format(
            "jdbc:postgresql://%s:%s/%s",
            DatabaseEnvironmentVariable.POSTGRES_HOST.getValue(),
            DatabaseEnvironmentVariable.POSTGRES_PORT.getValue(),
            DatabaseEnvironmentVariable.POSTGRES_DATABASE.getValue()),
        DatabaseEnvironmentVariable.POSTGRES_USER.getValue(),
        DatabaseEnvironmentVariable.POSTGRES_PASSWORD.getValue());
    jdbi.installPlugin(new SqlObjectPlugin());
    return jdbi;
  }
}
