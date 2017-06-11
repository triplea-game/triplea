package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.config.GameEnginePropertyReader;


/**
 * Immutable data class that reads a configuration file and stores the location of where
 * to find the "triplea_map.xml" file, whether that be a URL, or a local file path relative to
 * the game root folder.
 *
 * <p>
 * Can be used to create a <code>MapDownloadAction</code>
 * </p>
 */
public class MapListingSource {
  private final String mapListDownloadSite;

  public MapListingSource(final GameEnginePropertyReader propertyReader) {
    checkNotNull(propertyReader);
    mapListDownloadSite = propertyReader.readMapListingDownloadUrl();
  }

  /** Return the URL where we can download a file that lists each map that is available. */
  protected String getMapListDownloadSite() {
    return mapListDownloadSite;
  }
}
