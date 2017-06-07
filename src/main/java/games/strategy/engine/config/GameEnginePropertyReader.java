package games.strategy.engine.config;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.util.Version;

/**
 * Reads property values from the game engine configuration file.
 */
public class GameEnginePropertyReader extends PropertyFileReader {

  public static final String GAME_ENGINE_PROPERTY_FILE = "game_engine.properties";

  public GameEnginePropertyReader() {
    super(GAME_ENGINE_PROPERTY_FILE);
  }

  @VisibleForTesting GameEnginePropertyReader(File propFile) {
    super(propFile);
  }

  public Version readEngineVersion() {
    return new Version(readProperty(GameEngineProperty.ENGINE_VERSION));
  }

  public String readLobbyPropertiesUrl() {
    final String url = readProperty(GameEngineProperty.LOBBY_PROPS_URL);
    try {
      return getUrlFollowingRedirects(url);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get download file url: " + url, e);
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




  @VisibleForTesting
  interface GameEngineProperty {
    String MAP_LISTING_FILE = "Map_List_File";
    String MAP_LISTING_SOURCE_FILE = "Map_List_File";
    String ENGINE_VERSION = "engine_version";
    String LOBBY_PROPS_URL = "lobby_properties_file_url";
    String LOBBY_PROPS_BACKUP_FILE = "lobby_properties_file_backup";
  }
}
