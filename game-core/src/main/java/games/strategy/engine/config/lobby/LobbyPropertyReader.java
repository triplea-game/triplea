package games.strategy.engine.config.lobby;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.config.FilePropertyReader;
import games.strategy.engine.config.PropertyReader;

/**
 * Reads property values from the lobby configuration file.
 */
public final class LobbyPropertyReader {
  private static final String LOBBY_PROPERTIES_FILE = "config/lobby/lobby.properties";

  private final PropertyReader propertyReader;

  public LobbyPropertyReader() {
    this(new FilePropertyReader(LOBBY_PROPERTIES_FILE));
  }

  @VisibleForTesting
  public LobbyPropertyReader(final PropertyReader propertyReader) {
    checkNotNull(propertyReader);

    this.propertyReader = propertyReader;
  }

  public int getPort() {
    return Integer.parseInt(propertyReader.readProperty(PropertyKeys.PORT));
  }

  public String getPostgresDatabase() {
    final String value = propertyReader.readProperty(PropertyKeys.POSTGRES_DATABASE);
    return !value.isEmpty() ? value : DefaultValues.POSTGRES_DATABASE;
  }

  public String getPostgresHost() {
    final String value = propertyReader.readProperty(PropertyKeys.POSTGRES_HOST);
    return !value.isEmpty() ? value : DefaultValues.POSTGRES_HOST;
  }

  public String getPostgresPassword() {
    return propertyReader.readProperty(PropertyKeys.POSTGRES_PASSWORD);
  }

  public int getPostgresPort() {
    final String value = propertyReader.readProperty(PropertyKeys.POSTGRES_PORT);
    return !value.isEmpty() ? Integer.parseInt(value) : DefaultValues.POSTGRES_PORT;
  }

  public String getPostgresUser() {
    return propertyReader.readProperty(PropertyKeys.POSTGRES_USER);
  }

  public boolean isMaintenanceMode() {
    return Boolean.parseBoolean(propertyReader.readProperty(PropertyKeys.MAINTENANCE_MODE));
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
    String POSTGRES_DATABASE = "ta_users";
    String POSTGRES_HOST = "localhost";
    int POSTGRES_PORT = 5432;
  }
}
