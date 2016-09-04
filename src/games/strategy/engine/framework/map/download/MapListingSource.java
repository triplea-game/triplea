package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.HttpURLConnection;
import java.net.URL;

import games.strategy.engine.config.GameEngineProperty;
import games.strategy.engine.config.PropertyReader;


/**
 * Immutable data class that reads a configuration file and stores the location of where
 * to find the "triplea_map.xml" file, whether that be a URL, or a local file path relative to
 * the game root folder.
 *
 * Can be used to create a <code>MapDownloadAction</code>
 */
public class MapListingSource {
  private final String mapListDownloadSite;

  public MapListingSource(final PropertyReader propertyReader) {
    checkNotNull(propertyReader);
    mapListDownloadSite = propertyReader.readProperty(GameEngineProperty.MAP_LISTING_SOURCE_FILE);
  }

  /** Return the URL where we can download a file that lists each map that is available */
  protected String getMapListDownloadSite() {
    return mapListDownloadSite;
  }

  /** Return the URL where we can download a file that lists each map that is available */
  public URL getMapListDownloadURL() {
    try {
      return getUrlFollowingRedirects(mapListDownloadSite);
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to download: " + mapListDownloadSite, e);
    }
  }

  private static URL getUrlFollowingRedirects(final String possibleRedirectionUrl) throws Exception {
    URL url = new URL(possibleRedirectionUrl);
    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    final int status = conn.getResponseCode();
    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
        || status == HttpURLConnection.HTTP_SEE_OTHER) {
      // update the URL if we were redirected
      url = new URL(conn.getHeaderField("Location"));
    }
    return url;
  }
}
