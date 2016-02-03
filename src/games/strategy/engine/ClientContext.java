package games.strategy.engine;

import java.io.File;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.mapDownload.MapDownloadController;
import games.strategy.engine.framework.mapDownload.MapListingSource;

/**
 * IOC container for storing objects needed by the TripleA Swing client
 * A full blow dependency injection framework would deprecate this class.
 *
 * This class roughly follows the singleton pattern. The singleton instance
 * can be updated, this is allowed to enable a mock instance of this class to
 * be used.
 */
public final class ClientContext {

  private static ClientContext instance = new ClientContext();



  public static ClientContext getInstance() {
    return instance;
  }

  /** Useful for testing, not meant for normal code paths */
  public static void setMockHandler( ClientContext mockHandler) {
    instance = mockHandler;
  }


  private final MapDownloadController mapDownloadController;


  private ClientContext() {
    File mapDownloadPropertiesFile = new File(GameRunner2.getRootFolder(), "mapDownload.properties");
    MapListingSource listingSource = new MapListingSource(mapDownloadPropertiesFile);
    mapDownloadController = new MapDownloadController(listingSource);
  }


  public MapDownloadController mapDownloadController() {
    return mapDownloadController;
  }


}
