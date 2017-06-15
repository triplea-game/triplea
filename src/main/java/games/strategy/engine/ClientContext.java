package games.strategy.engine;

import games.strategy.engine.config.GameEnginePropertyReader;
import games.strategy.engine.framework.map.download.DownloadCoordinator;
import games.strategy.engine.framework.map.download.MapDownloadController;
import games.strategy.engine.framework.map.download.MapListingSource;
import games.strategy.triplea.settings.ai.AiSettings;
import games.strategy.triplea.settings.battle.calc.BattleCalcSettings;
import games.strategy.triplea.settings.battle.options.BattleOptionsSettings;
import games.strategy.triplea.settings.folders.FolderSettings;
import games.strategy.triplea.settings.scrolling.ScrollSettings;

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
  private static ClientContext instance = new ClientContext();

  private final MapDownloadController mapDownloadController;
  private final EngineVersion engineVersion;
  private final GameEnginePropertyReader gameEnginePropertyReader;
  private final MapListingSource mapListingSource;
  private final ScrollSettings scrollSettings;
  private final FolderSettings folderSettings;
  private final AiSettings aiSettings;
  private final BattleCalcSettings battleCalcSettings;
  private final BattleOptionsSettings battleOptionsSettings;

  private final DownloadCoordinator downloadCoordinator = new DownloadCoordinator();

  private ClientContext() {
    gameEnginePropertyReader = new GameEnginePropertyReader();
    mapListingSource = new MapListingSource(gameEnginePropertyReader);
    mapDownloadController = new MapDownloadController(mapListingSource);
    engineVersion = new EngineVersion(gameEnginePropertyReader);
    scrollSettings = new ScrollSettings();
    folderSettings = new FolderSettings();
    aiSettings = new AiSettings();
    battleCalcSettings = new BattleCalcSettings();
    battleOptionsSettings = new BattleOptionsSettings();
  }

  public static GameEnginePropertyReader gameEnginePropertyReader() {
    return instance.gameEnginePropertyReader;
  }

  public static MapListingSource mapListingSource() {
    return instance.mapListingSource;
  }

  public static DownloadCoordinator downloadCoordinator() {
    return instance.downloadCoordinator;
  }

  public static MapDownloadController mapDownloadController() {
    return instance.mapDownloadController;
  }

  public static EngineVersion engineVersion() {
    return instance.engineVersion;
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
}
