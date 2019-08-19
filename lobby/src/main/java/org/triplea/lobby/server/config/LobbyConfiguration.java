package org.triplea.lobby.server.config;

import lombok.Getter;
import org.triplea.lobby.server.EnvironmentVariable;
import org.triplea.lobby.server.db.Database;
import org.triplea.lobby.server.db.DatabaseDao;
import org.triplea.lobby.server.db.DatabaseEnvironmentVariable;

/** Provides access to the lobby configuration. */
@Getter
public final class LobbyConfiguration {

  private final int port;
  private final DatabaseDao databaseDao;

  public LobbyConfiguration() {
    port = Integer.parseInt(EnvironmentVariable.PORT.getValue());
    databaseDao =
        Database.builder()
            .postgresDatabase(DatabaseEnvironmentVariable.POSTGRES_DATABASE.getValue())
            .postgresHost(DatabaseEnvironmentVariable.POSTGRES_HOST.getValue())
            .postgresPassword(DatabaseEnvironmentVariable.POSTGRES_PASSWORD.getValue())
            .postgresUser(DatabaseEnvironmentVariable.POSTGRES_USER.getValue())
            .postgresPort(Integer.valueOf(DatabaseEnvironmentVariable.POSTGRES_PORT.getValue()))
            .build()
            .newDatabaseDao();
  }
}
