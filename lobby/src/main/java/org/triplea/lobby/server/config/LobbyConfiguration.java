package org.triplea.lobby.server.config;

import org.triplea.lobby.server.EnvironmentVariable;

import lombok.Getter;

/**
 * Provides access to the lobby configuration.
 */
@Getter
public final class LobbyConfiguration {

  private final int port;
  private final String postgresHost;
  private final int postgresPort;
  private final String postgresDatabase;
  private final String postgresUser;
  private final String postgresPassword;

  public LobbyConfiguration() {
    postgresDatabase = "ta_users";
    postgresHost = EnvironmentVariable.POSTGRES_HOST.getValue();
    postgresPassword = EnvironmentVariable.POSTGRES_PASSWORD.getValue();
    postgresUser = EnvironmentVariable.POSTGRES_USER.getValue();
    port = Integer.valueOf(EnvironmentVariable.PORT.getValue());
    postgresPort = Integer.valueOf(EnvironmentVariable.POSTGRES_PORT.getValue());
  }
}
