package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

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
  static final class PropertyNames {
    private PropertyNames() {}

    static final String NAME = "name";
    static final String VERSION = "version";
  }

  @VisibleForTesting
  static final long CURRENT_SCHEMA_VERSION = 1L;

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

  private static Map<ExportOptionName, Object> newDefaultExportOptionsByName() {
    return Maps.immutableEnumMap(ImmutableMap.of(
        ExportOptionName.EXCLUDE_DELEGATES, false));
  }

  /**
   * Creates a new game data memento exporter using the default export options.
   *
   * @return A new game data memento exporter; never {@code null}.
   */
  public static MementoExporter<GameData> newExporter() {
    return newExporter(DEFAULT_EXPORT_OPTIONS_BY_NAME);
  }

  /**
   * Creates a new game data memento exporter using the specified export options.
   *
   * @param optionsByName The memento export options; must not be {@code null}. The key is the option name. The value is
   *        the option value.
   *
   * @return A new game data memento exporter; never {@code null}.
   */
  public static MementoExporter<GameData> newExporter(final Map<ExportOptionName, Object> optionsByName) {
    checkNotNull(optionsByName);

    return new PropertyBagMementoExporter<>(SCHEMA_ID, CURRENT_SCHEMA_VERSION, new ExportHandlers(optionsByName));
  }

  /**
   * Names the options that can be specified when creating a new game data memento exporter.
   */
  public static enum ExportOptionName {
    /**
     * Indicates delegates should be excluded from the memento (value type: {@code Boolean}).
     */
    EXCLUDE_DELEGATES
  }

  private static final class ExportHandlers implements PropertyBagMementoExporter.HandlerSupplier<GameData> {
    private final Map<Long, PropertyBagMementoExporter.Handler<GameData>> handlersBySchemaVersion =
        getHandlersBySchemaVersion();

    // TODO: add support for options
    @SuppressWarnings("unused")
    private final Map<ExportOptionName, Object> optionsByName;

    ExportHandlers(final Map<ExportOptionName, Object> optionsByName) {
      this.optionsByName = new HashMap<>(optionsByName);
    }

    private Map<Long, PropertyBagMementoExporter.Handler<GameData>> getHandlersBySchemaVersion() {
      return ImmutableMap.<Long, PropertyBagMementoExporter.Handler<GameData>>builder()
          .put(1L, this::exportPropertiesV1)
          .build();
    }

    private void exportPropertiesV1(final GameData gameData, final Map<String, Object> propertiesByName) {
      propertiesByName.put(PropertyNames.NAME, gameData.getGameName());
      propertiesByName.put(PropertyNames.VERSION, gameData.getGameVersion());
      // TODO: handle remaining properties
    }

    @Override
    public Optional<PropertyBagMementoExporter.Handler<GameData>> getHandler(final long schemaVersion) {
      return Optional.ofNullable(handlersBySchemaVersion.get(schemaVersion));
    }
  }

  /**
   * Creates a new game data memento importer.
   *
   * @return A new game data memento importer; never {@code null}.
   */
  public static MementoImporter<GameData> newImporter() {
    return new PropertyBagMementoImporter<>(SCHEMA_ID, new ImportHandlers());
  }

  private static final class ImportHandlers implements PropertyBagMementoImporter.HandlerSupplier<GameData> {
    private final Map<Long, PropertyBagMementoImporter.Handler<GameData>> handlersBySchemaVersion =
        getHandlersBySchemaVersion();

    private Map<Long, PropertyBagMementoImporter.Handler<GameData>> getHandlersBySchemaVersion() {
      return ImmutableMap.<Long, PropertyBagMementoImporter.Handler<GameData>>builder()
          .put(1L, this::importPropertiesV1)
          .build();
    }

    private GameData importPropertiesV1(final Map<String, Object> propertiesByName) throws MementoImportException {
      final GameData gameData = new GameData();
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

    @Override
    public Optional<PropertyBagMementoImporter.Handler<GameData>> getHandler(final long schemaVersion) {
      return Optional.ofNullable(handlersBySchemaVersion.get(schemaVersion));
    }
  }
}
