package org.triplea.lobby.server.config;

import org.triplea.lobby.server.EnvironmentVariable;
import org.triplea.lobby.server.db.Database;
import org.triplea.lobby.server.db.DatabaseDao;

import lombok.Getter;

/**
 * Provides access to the lobby configuration.
 */
@Getter
public final class LobbyConfiguration {

  private final int port;
  private final DatabaseDao databaseDao;

  public LobbyConfiguration() {
    port = Integer.valueOf(EnvironmentVariable.PORT.getValue());
    databaseDao = Database.builder()
        .postgresDatabase("ta_users")
        .postgresHost(EnvironmentVariable.POSTGRES_HOST.getValue())
        .postgresPassword(EnvironmentVariable.POSTGRES_PASSWORD.getValue())
        .postgresUser(EnvironmentVariable.POSTGRES_USER.getValue())
        .postgresPort(Integer.valueOf(EnvironmentVariable.POSTGRES_PORT.getValue()))
        .build()
        .newDatabaseDao();
  }
}
