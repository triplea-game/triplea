package games.strategy.engine;

import java.util.List;

import games.strategy.engine.config.client.GameEnginePropertyReader;
import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.DownloadRunnable;
import games.strategy.engine.framework.map.download.DownloadCoordinator;
import games.strategy.engine.framework.map.download.MapDownloadController;
import games.strategy.triplea.settings.ai.AiSettings;
import games.strategy.triplea.settings.battle.calc.BattleCalcSettings;
import games.strategy.triplea.settings.battle.options.BattleOptionsSettings;
import games.strategy.triplea.settings.folders.FolderSettings;
import games.strategy.triplea.settings.scrolling.ScrollSettings;
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
  private final ScrollSettings scrollSettings = new ScrollSettings();
  private final FolderSettings folderSettings = new FolderSettings();
  private final AiSettings aiSettings = new AiSettings();
  private final BattleCalcSettings battleCalcSettings = new BattleCalcSettings();
  private final BattleOptionsSettings battleOptionsSettings = new BattleOptionsSettings();
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

  public static ScrollSettings scrollSettings() {
    return instance.scrollSettings;
  }

  public static FolderSettings folderSettings() {
    return instance.folderSettings;
  }

  public static AiSettings aiSettings() {
    return instance.aiSettings;
  }

  public static BattleCalcSettings battleCalcSettings() {
    return instance.battleCalcSettings;
  }

  public static BattleOptionsSettings battleOptionsSettings() {
    return instance.battleOptionsSettings;
  }

  public static Version engineVersion() {
    return instance.gameEnginePropertyReader.getEngineVersion();
  }

  public static List<DownloadFileDescription> getMapDownloadList() {
    final String mapDownloadListUrl = instance.gameEnginePropertyReader.getMapListingSource();
    return new DownloadRunnable(mapDownloadListUrl).getDownloads();
  }
}
