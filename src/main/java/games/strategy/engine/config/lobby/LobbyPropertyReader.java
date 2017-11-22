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

  public String getPostgresUser() {
    return propertyReader.readProperty(PropertyKeys.POSTGRES_USER);
  }

  public String getPostgresPassword() {
    return propertyReader.readProperty(PropertyKeys.POSTGRES_PASSWORD);
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
    String POSTGRES_USER = "postgres_user";
    String POSTGRES_PASSWORD = "postgres_password";
  }
}
