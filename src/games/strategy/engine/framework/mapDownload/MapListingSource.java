package games.strategy.engine.framework.mapDownload;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.config.GameEngineProperty;
import games.strategy.engine.config.PropertyReader;


/**
 * Immutable data class that reads a configuration file and stores the location of where
 *  to find the "triplea_map.xml" file, whether that be a URL, or a local file path relative to
 *  the game root folder.
 *
 * Can be used to create a <code>MapDownloadAction</code>
 */
public class MapListingSource {
  private final String mapListDownloadSite;

  public MapListingSource(PropertyReader propertyReader) {
    checkNotNull(propertyReader);
    mapListDownloadSite = propertyReader.readProperty(GameEngineProperty.MAP_LISTING_SOURCE_FILE);
  }

  /** Return the URL where we can download a file that lists each map that is available */
  protected String getMapListDownloadSite() {
    return mapListDownloadSite;
  }

}
