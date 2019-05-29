package org.triplea.server.error.reporting;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility to get connections to the Postgres lobby database.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Database {
  static Jdbi newConnection() {
    final Jdbi jdbi = Jdbi.create(
        "jdbc:postgresql://localhost:5432/lobby", "postgres", "postgres");
    jdbi.installPlugin(new SqlObjectPlugin());
    return jdbi;
  }
}
