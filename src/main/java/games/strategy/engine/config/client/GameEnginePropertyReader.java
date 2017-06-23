package games.strategy.engine.config.client;

import java.io.IOException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.config.PropertyFileReader;
import games.strategy.engine.config.client.backup.BackupPropertyFetcher;
import games.strategy.engine.config.client.remote.LobbyServerPropertiesFetcher;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.util.Version;

/**
 * Reads property values from the game engine configuration file.
 */
public class GameEnginePropertyReader {

  public static final String GAME_ENGINE_PROPERTY_FILE = "game_engine.properties";

  private final PropertyFileReader propertyFileReader;
  private final LobbyServerPropertiesFetcher lobbyServerPropertiesFetcher;
  private final BackupPropertyFetcher backupPropertyFetcher;


  /**
   * Default constructor that reads the client configuration properties file.
   */
  public GameEnginePropertyReader() {
    this(
        new PropertyFileReader(GAME_ENGINE_PROPERTY_FILE),
        new LobbyServerPropertiesFetcher(),
        new BackupPropertyFetcher());
  }

  @VisibleForTesting
  GameEnginePropertyReader(
      final PropertyFileReader propertyFileReader,
      final LobbyServerPropertiesFetcher lobbyServerPropertiesFetcher,
      final BackupPropertyFetcher backupPropertyFetcher) {
    this.propertyFileReader = propertyFileReader;
    this.lobbyServerPropertiesFetcher = lobbyServerPropertiesFetcher;
    this.backupPropertyFetcher = backupPropertyFetcher;
  }

  /**
   * Fetches LobbyServerProperties based on values read from configuration.
   * LobbyServerProperties are based on a properties file hosted on github which can be updated live.
   * This properties file tells the game client how to connect to the lobby and provides a welcome message.
   * In case Github is not available, we also have a backup hardcoded lobby host address in the client
   * configuration that we would pass back in case github is not available.
   *
   * @return LobbyServerProperties as fetched and parsed from github hosted remote URL.
   *     Otherwise backup values from client config.
   */
  public LobbyServerProperties fetchLobbyServerProperties() {
    final String lobbyPropsUrl =
        propertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.LOBBY_PROP_FILE_URL);

    final Version currentVersion = new Version(propertyFileReader.readProperty(PropertyKeys.ENGINE_VERSION));

    try {
      return lobbyServerPropertiesFetcher.downloadAndParseRemoteFile(lobbyPropsUrl, currentVersion);
    } catch (final IOException e) {
      ClientLogger.logError(
          String.format("Failed to download lobby server props file from %s; will attempt to use a local backup.",
              lobbyPropsUrl),
          e);
      final String backupAddress =
          propertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.LOBBY_BACKUP_HOST_ADDRESS);

      return backupPropertyFetcher.parseBackupValuesFromEngineConfig(backupAddress);
    }
  }

  public Version getEngineVersion() {
    return new Version(propertyFileReader.readProperty(PropertyKeys.ENGINE_VERSION));
  }

  public String getMapListingSource() {
    return propertyFileReader.readProperty(PropertyKeys.MAP_LISTING_SOURCE_FILE);
  }

  /**
   * Reads the max memory user setting from configuration and returns a result object.
   * The result object contains an 'isSet' flag, and the value set by user if set.
   */
  public MaxMemorySetting readMaxMemory() {
    final String value = propertyFileReader.readProperty(PropertyKeys.MAX_MEMORY);

    if (Strings.nullToEmpty(value).isEmpty()) {
      return MaxMemorySetting.NOT_SET;
    }

    return MaxMemorySetting.of(value);
  }

  @VisibleForTesting
  interface PropertyKeys {
    String MAP_LISTING_SOURCE_FILE = "map_list_file";
    String ENGINE_VERSION = "engine_version";
    String LOBBY_PROP_FILE_URL = "lobby_properties_file_url";
    String LOBBY_BACKUP_HOST_ADDRESS = "lobby_backup_url";
    String MAX_MEMORY = "max_memory";
  }
}
