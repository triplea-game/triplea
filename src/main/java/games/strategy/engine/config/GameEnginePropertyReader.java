package games.strategy.engine.config;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import games.strategy.util.Version;

/**
 * Reads property values from the game engine configuration file.
 */
public class GameEnginePropertyReader extends PropertyFileReader {

  public static final String GAME_ENGINE_PROPERTY_FILE = "game_engine.properties";

  public GameEnginePropertyReader() {
    super(GAME_ENGINE_PROPERTY_FILE);
  }

  @VisibleForTesting
  GameEnginePropertyReader(final File propFile) {
    super(propFile);
  }

  public Version readEngineVersion() {
    return new Version(readProperty(GameEngineProperty.ENGINE_VERSION));
  }

  /**
   * Does a network fetch for a lobby properties file. That file has the IP
   * address for the lobby (and other connectivity information)
   */
  public String readLobbyPropertiesUrl() {
    final String url = readProperty(GameEngineProperty.LOBBY_PROPS_URL);
    try {
      return getUrlFollowingRedirects(url);
    } catch (final Exception e) {
      throw new NetworkFetchException(url, e);
    }
  }

  private static class NetworkFetchException extends RuntimeException {
    private static final long serialVersionUID = -8963534515318619962L;

    NetworkFetchException(final String url, final Exception exception) {
      super("Failed to get download file url: " + url, exception);
    }
  }

  private static String getUrlFollowingRedirects(final String possibleRedirectionUrl) throws Exception {
    URL url = new URL(possibleRedirectionUrl);
    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    final int status = conn.getResponseCode();
    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
        || status == HttpURLConnection.HTTP_SEE_OTHER) {
      // update the URL if we were redirected
      url = new URL(conn.getHeaderField("Location"));
    }
    return url.toString();
  }


  public String readMapListingDownloadUrl() {
    return super.readProperty(GameEngineProperty.MAP_LISTING_SOURCE_FILE);
  }

  public String readLobbyPropertiesBackupFile() {
    return super.readProperty(GameEngineProperty.LOBBY_PROPS_BACKUP_FILE);
  }


  public MaxMemorySetting readMaxMemory() {
    final String value = super.readProperty(GameEngineProperty.MAX_MEMORY);

    if (Strings.nullToEmpty(value).isEmpty()) {
      return MaxMemorySetting.NOT_SET;
    }

    return MaxMemorySetting.of(value);
  }

  public boolean useExperimentalUi() {
    return String.valueOf(true).equals(super.readProperty(GameEngineProperty.JAVAFX_UI, false));
  }


  @VisibleForTesting
  interface GameEngineProperty {
    String MAP_LISTING_FILE = "Map_List_File";
    String MAP_LISTING_SOURCE_FILE = "Map_List_File";
    String ENGINE_VERSION = "engine_version";
    String LOBBY_PROPS_URL = "lobby_properties_file_url";
    String LOBBY_PROPS_BACKUP_FILE = "lobby_properties_file_backup";
    String MAX_MEMORY = "max_memory";
    String JAVAFX_UI = "javafx_ui";
  }
}
