package org.triplea.lobby.server.config;

import static com.google.common.base.Preconditions.checkNotNull;

import org.triplea.common.config.PropertyReader;

import com.google.common.annotations.VisibleForTesting;

/**
 * Reads property values from a lobby configuration.
 */
public final class LobbyPropertyReader {
  private final PropertyReader propertyReader;

  public LobbyPropertyReader(final PropertyReader propertyReader) {
    checkNotNull(propertyReader);

    this.propertyReader = propertyReader;
  }

  public int getPort() {
    return propertyReader.readIntegerPropertyOrDefault(PropertyKeys.PORT, DefaultValues.PORT);
  }

  public String getPostgresDatabase() {
    return propertyReader.readPropertyOrDefault(PropertyKeys.POSTGRES_DATABASE, DefaultValues.POSTGRES_DATABASE);
  }

  public String getPostgresHost() {
    return propertyReader.readPropertyOrDefault(PropertyKeys.POSTGRES_HOST, DefaultValues.POSTGRES_HOST);
  }

  public String getPostgresPassword() {
    return propertyReader.readProperty(PropertyKeys.POSTGRES_PASSWORD);
  }

  public int getPostgresPort() {
    return propertyReader.readIntegerPropertyOrDefault(PropertyKeys.POSTGRES_PORT, DefaultValues.POSTGRES_PORT);
  }

  public String getPostgresUser() {
    return propertyReader.readProperty(PropertyKeys.POSTGRES_USER);
  }

  public boolean isMaintenanceMode() {
    return propertyReader.readBooleanPropertyOrDefault(PropertyKeys.MAINTENANCE_MODE, DefaultValues.MAINTENANCE_MODE);
  }

  /**
   * The valid lobby property keys.
   */
  @VisibleForTesting
  public interface PropertyKeys {
    String MAINTENANCE_MODE = "maintenance_mode";
    String PORT = "port";
    String POSTGRES_DATABASE = "postgres_database";
    String POSTGRES_HOST = "postgres_host";
    String POSTGRES_PASSWORD = "postgres_password";
    String POSTGRES_PORT = "postgres_port";
    String POSTGRES_USER = "postgres_user";
  }

  @VisibleForTesting
  interface DefaultValues {
    boolean MAINTENANCE_MODE = false;
    int PORT = 3304;
    String POSTGRES_DATABASE = "ta_users";
    String POSTGRES_HOST = "localhost";
    int POSTGRES_PORT = 5432;
  }
}
