package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import games.strategy.engine.framework.IGameLoader;
import games.strategy.util.Version;
import games.strategy.util.memento.MementoExporter;
import games.strategy.util.memento.MementoImportException;
import games.strategy.util.memento.MementoImporter;
import games.strategy.util.memento.PropertyBagMementoExporter;
import games.strategy.util.memento.PropertyBagMementoImporter;

/**
 * Provides factory methods for creating objects that can import mementos to and export mementos from game data.
 */
public final class GameDataMemento {
  @VisibleForTesting
  static final String SCHEMA_ID = "application/x.triplea.game-data-memento";

  /**
   * An immutable collection of default memento export options.
   *
   * <p>
   * The key is the option name. The value is the option value.
   * </p>
   */
  public static final Map<ExportOptionName, Object> DEFAULT_EXPORT_OPTIONS_BY_NAME = newDefaultExportOptionsByName();

  private GameDataMemento() {}

  @VisibleForTesting
  interface PropertyNames {
    String DICE_SIDES = "diceSides";
    String LOADER = "loader";
    String NAME = "name";
    String VERSION = "version";
  }

  private static Map<ExportOptionName, Object> newDefaultExportOptionsByName() {
    return Maps.immutableEnumMap(ImmutableMap.of(
        ExportOptionName.EXCLUDE_DELEGATES, false));
  }

  /**
   * Creates a new game data memento exporter using the default export options.
   *
   * @return A new game data memento exporter.
   */
  public static MementoExporter<GameData> newExporter() {
    return newExporterInternal();
  }

  /**
   * Creates a new game data memento exporter using the specified export options.
   *
   * @param optionsByName The memento export options. The key is the option name. The value is the option value.
   *
   * @return A new game data memento exporter.
   */
  public static MementoExporter<GameData> newExporter(final Map<ExportOptionName, Object> optionsByName) {
    checkNotNull(optionsByName);

    return newExporterInternal(optionsByName);
  }

  @VisibleForTesting
  static PropertyBagMementoExporter<GameData> newExporterInternal() {
    return newExporterInternal(DEFAULT_EXPORT_OPTIONS_BY_NAME);
  }

  @VisibleForTesting
  static PropertyBagMementoExporter<GameData> newExporterInternal(final Map<ExportOptionName, Object> optionsByName) {
    return new PropertyBagMementoExporter<>(SCHEMA_ID, new ExportHandler(optionsByName));
  }

  /**
   * Names the options that can be specified when creating a new game data memento exporter.
   */
  public enum ExportOptionName {
    /**
     * Indicates delegates should be excluded from the memento (value type: {@code Boolean}).
     */
    EXCLUDE_DELEGATES
  }

  private static final class ExportHandler implements PropertyBagMementoExporter.Handler<GameData> {
    // TODO: add support for options
    @SuppressWarnings("unused")
    private final Map<ExportOptionName, Object> optionsByName;

    ExportHandler(final Map<ExportOptionName, Object> optionsByName) {
      this.optionsByName = new HashMap<>(optionsByName);
    }

    @Override
    public void exportProperties(final GameData gameData, final Map<String, Object> propertiesByName) {
      propertiesByName.put(PropertyNames.DICE_SIDES, gameData.getDiceSides());
      propertiesByName.put(PropertyNames.LOADER, gameData.getGameLoader());
      propertiesByName.put(PropertyNames.NAME, gameData.getGameName());
      propertiesByName.put(PropertyNames.VERSION, gameData.getGameVersion());
      // TODO: handle remaining properties
    }
  }

  /**
   * Creates a new game data memento importer.
   *
   * @return A new game data memento importer.
   */
  public static MementoImporter<GameData> newImporter() {
    return new PropertyBagMementoImporter<>(SCHEMA_ID, new ImportHandler());
  }

  private static final class ImportHandler implements PropertyBagMementoImporter.Handler<GameData> {
    @Override
    public GameData importProperties(final Map<String, Object> propertiesByName) throws MementoImportException {
      final GameData gameData = new GameData();
      gameData.setDiceSides(getRequiredProperty(propertiesByName, PropertyNames.DICE_SIDES, Integer.class));
      gameData.setGameLoader(getRequiredProperty(propertiesByName, PropertyNames.LOADER, IGameLoader.class));
      gameData.setGameName(getRequiredProperty(propertiesByName, PropertyNames.NAME, String.class));
      gameData.setGameVersion(getRequiredProperty(propertiesByName, PropertyNames.VERSION, Version.class));
      // TODO: handle remaining properties
      return gameData;
    }

    private static <T> T getRequiredProperty(
        final Map<String, Object> propertiesByName,
        final String name,
        final Class<T> type) throws MementoImportException {
      assert propertiesByName != null;
      assert name != null;
      assert type != null;

      if (!propertiesByName.containsKey(name)) {
        throw new MementoImportException(String.format("memento is missing required property '%s'", name));
      }

      try {
        return type.cast(propertiesByName.get(name));
      } catch (final ClassCastException e) {
        throw new MementoImportException(String.format("memento property '%s' has wrong type", name), e);
      }
    }
  }
}
