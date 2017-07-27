package games.strategy.engine;

import java.util.List;

import games.strategy.engine.config.client.GameEnginePropertyReader;
import games.strategy.engine.framework.map.download.DownloadCoordinator;
import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.DownloadRunnable;
import games.strategy.engine.framework.map.download.MapDownloadController;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.Version;

/**
 * Manages the creation of objects, similar to a dependency injection framework.
 * Use this class to manage singletons and as a factory to create objects that have shared
 * dependencies already managed by this class.
 * Example usage:
 *
 * <pre>
 * <code>
 *   // before
 *   public void clientCode(SharedDependency sharedDependencyWiredThroughAllTheMethods) {
 *     swingStuff(sharedDependencyWiredThroughAllTheMethods);
 *     :
 *   }
 *   private void swingStuff(SharedDependency sharedDependencyWiredThroughAllTheMethods) {
 *     int preferenceValue = new UserSetting(sharedDependencyWiredThroughAllTheMethods).getNumberPreference();
 *     :
 *   }
 *
 *   // after
 *   public void clientCode() {
 *     doSwingStuff(ClientContext.userSettings());
 *     :
 *   }
 *
 *   private void doSwingStuff(UserSettings settings) {
 *     int preferenceValue = settings.getNumberPreference();
 *     :
 *   }
 * </code>
 * </pre>
 */
public final class ClientContext {
  private static final ClientContext instance = new ClientContext();

  private final GameEnginePropertyReader gameEnginePropertyReader = new GameEnginePropertyReader();
  private final MapDownloadController mapDownloadController = new MapDownloadController();
  private final DownloadCoordinator downloadCoordinator = new DownloadCoordinator();

  private ClientContext() {}

  public static GameEnginePropertyReader gameEnginePropertyReader() {
    return instance.gameEnginePropertyReader;
  }

  public static DownloadCoordinator downloadCoordinator() {
    return instance.downloadCoordinator;
  }

  public static MapDownloadController mapDownloadController() {
    return instance.mapDownloadController;
  }

  public static Version engineVersion() {
    return instance.gameEnginePropertyReader.getEngineVersion();
  }

  public static List<DownloadFileDescription> getMapDownloadList() {
    final String mapDownloadListUrl =
        (ClientSetting.MAP_LIST_OVERRIDE.isSet()) ? ClientSetting.MAP_LIST_OVERRIDE.value()
            : UrlConstants.MAP_DOWNLOAD_LIST.toString();

    return new DownloadRunnable(mapDownloadListUrl).getDownloads();
  }
}
