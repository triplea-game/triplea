package games.strategy.engine.config.client;

import java.io.IOException;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.config.PropertyFileReader;
import games.strategy.engine.config.client.backup.BackupPropertyFetcher;
import games.strategy.engine.config.client.remote.LobbyServerPropertiesFetcher;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.triplea.settings.ClientSetting;
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
   *         Otherwise backup values from client config.
   */
  public LobbyServerProperties fetchLobbyServerProperties() {
    // props from override
    if (ClientSetting.TEST_LOBBY_HOST.isSet()) {
      return new LobbyServerProperties(
          ClientSetting.TEST_LOBBY_HOST.value(),
          ClientSetting.TEST_LOBBY_PORT.intValue());
    }

    // normal case, download props file that will tell us where the lobby is
    final String lobbyPropsUrl =
        propertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.LOBBY_PROP_FILE_URL);
    final Version currentVersion = new Version(propertyFileReader.readProperty(PropertyKeys.ENGINE_VERSION));

    try {
      return lobbyServerPropertiesFetcher.downloadAndParseRemoteFile(lobbyPropsUrl, currentVersion);
    } catch (final IOException e) {

      if (!ClientSetting.LOBBY_LAST_USED_HOST.isSet()) {
        ClientLogger.logError(
            String.format("Failed to download lobby server property file from %s; "
                    + "Please verify your internet connection and try again.",
                lobbyPropsUrl),
            e);
        throw new RuntimeException(e);
      }
      ClientLogger.logQuietly("Encountered an error while downloading lobby property file: " + lobbyPropsUrl
          + ", will attempt to connect to the lobby at its last known address. If this problem keeps happening, "
          + "you may be seeing network troubles, or the lobby may not be available.", e);

      // graceful recovery case, use the last lobby address we knew about
      return new LobbyServerProperties(
          ClientSetting.LOBBY_LAST_USED_HOST.value(),
          ClientSetting.LOBBY_LAST_USED_PORT.intValue());
    }
  }

  public Version getEngineVersion() {
    return new Version(propertyFileReader.readProperty(PropertyKeys.ENGINE_VERSION));
  }

  public String getMapListingSource() {
    if (ClientSetting.MAP_LIST_OVERRIDE.isSet()) {
      return ClientSetting.MAP_LIST_OVERRIDE.value();
    }
    return propertyFileReader.readProperty(PropertyKeys.MAP_LISTING_SOURCE_FILE);
  }

  public boolean useJavaFxUi() {
    return propertyFileReader.readProperty(PropertyKeys.JAVAFX_UI).equalsIgnoreCase(String.valueOf(true));
  }

  @VisibleForTesting
  interface PropertyKeys {
    String MAP_LISTING_SOURCE_FILE = "map_list_file";
    String ENGINE_VERSION = "engine_version";
    String LOBBY_PROP_FILE_URL = "lobby_properties_file_url";
    String JAVAFX_UI = "javafx_ui";
  }
}
