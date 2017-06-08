package games.strategy.engine;

import games.strategy.engine.config.GameEnginePropertyFileReader;
import games.strategy.engine.config.PropertyReader;
import games.strategy.engine.framework.map.download.MapDownloadController;
import games.strategy.engine.framework.map.download.MapListingSource;
import games.strategy.triplea.settings.ai.AiSettings;
import games.strategy.triplea.settings.battle.calc.BattleCalcSettings;
import games.strategy.triplea.settings.battle.options.BattleOptionsSettings;
import games.strategy.triplea.settings.folders.FolderSettings;
import games.strategy.triplea.settings.scrolling.ScrollSettings;

/**
 * IOC container for storing objects needed by the TripleA Swing client
 * A full blow dependency injection framework would deprecate this class.
 *
 * <p>
 * This class roughly follows the singleton pattern. The singleton instance
 * can be updated, this is allowed to enable a mock instance of this class to
 * be used.
 * </p>
 *
 * <p>
 * Caution: the public API of this class will grow to be fairly large. For every object we wish to return, we'll have an
 * "object()" method that will returns that same object. When things become hard to manage it'll be a good time
 * to move to an annotation or configuration based IOC framework.
 * </p>
 *
 * <p>
 * Second note, try to put as much class specific construction logic into the constructor of each class managed by this
 * container. This class should focus on just creating and wiring classes together. Contrast that with generating the
 * data
 * needed to create classes. For example, instead of parsing a file and passing that value to the constructor of another
 * class,
 * we would instead create an intermediary class that knows everything about which file to parse and how to parse it,
 * and we would
 * pass that intermediary class to the new class we wish to create. Said in another way, this class should not contain
 * any 'business'
 * logic.
 * </p>
 *
 * <p>
 * Third Note: Any classes created by ClientContext cannot call ClientContext in their constructor, all dependencies
 * must be passed to them.
 * Since GameRunner creates ClientContext, similar none of the classes created by Client Context can game runner 2
 * </p>
 */
public final class ClientContext {
  private static final ClientContext instance = new ClientContext();

  private final MapDownloadController mapDownloadController;
  private final EngineVersion engineVersion;
  private final PropertyReader propertyReader;
  private final MapListingSource mapListingSource;
  private final ScrollSettings scrollSettings;
  private final FolderSettings folderSettings;
  private final AiSettings aiSettings;
  private final BattleCalcSettings battleCalcSettings;
  private final BattleOptionsSettings battleOptionsSettings;

  private ClientContext() {
    propertyReader = new GameEnginePropertyFileReader();
    mapListingSource = new MapListingSource(propertyReader);
    mapDownloadController = new MapDownloadController(mapListingSource);
    engineVersion = new EngineVersion(propertyReader);
    scrollSettings = new ScrollSettings();
    folderSettings = new FolderSettings(propertyReader);
    aiSettings = new AiSettings();
    battleCalcSettings = new BattleCalcSettings();
    battleOptionsSettings = new BattleOptionsSettings();
  }

  public static PropertyReader propertyReader() {
    return instance.propertyReader;
  }

  public static MapListingSource mapListingSource() {
    return instance.mapListingSource;
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
