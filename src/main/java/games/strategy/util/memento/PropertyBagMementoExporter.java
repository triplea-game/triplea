package games.strategy.util.memento;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link MementoExporter} for instances of {@link PropertyBagMemento}.
 *
 * @param <T> The type of the memento originator.
 */
public final class PropertyBagMementoExporter<T> implements MementoExporter<T> {
  private final long defaultSchemaVersion;

  private final HandlerSupplier<T> handlerSupplier;

  private final String schemaId;

  /**
   * Initializes a new instance of the {@code PropertyBagMementoExporter} class.
   *
   * @param schemaId The schema identifier of the memento to export; must not be {@code null}.
   * @param defaultSchemaVersion The default schema version of the memento to export when the schema version is not
   *        explicitly specified.
   * @param handlerSupplier A supplier of export handlers for each supported schema version; must not be {@code null}.
   */
  public PropertyBagMementoExporter(
      final String schemaId,
      final long defaultSchemaVersion,
      final HandlerSupplier<T> handlerSupplier) {
    checkNotNull(schemaId);
    checkNotNull(handlerSupplier);

    this.defaultSchemaVersion = defaultSchemaVersion;
    this.handlerSupplier = handlerSupplier;
    this.schemaId = schemaId;
  }

  @Override
  public PropertyBagMemento exportMemento(final T originator) throws MementoExportException {
    return exportMemento(originator, defaultSchemaVersion);
  }

  /**
   * Exports a memento from the specified originator using the specified schema version of the memento.
   *
   * @param originator The memento originator; must not be {@code null}.
   * @param schemaVersion The schema version of the memento to export.
   *
   * @return The exported memento; never {@code null}.
   *
   * @throws MementoExportException If an error occurs while exporting the memento.
   */
  public PropertyBagMemento exportMemento(final T originator, final long schemaVersion) throws MementoExportException {
    checkNotNull(originator);

    final Handler<T> handler = handlerSupplier.getHandler(schemaVersion)
        .orElseThrow(() -> newUnsupportedSchemaVersionException(schemaVersion));
    final Map<String, Object> propertiesByName = new HashMap<>();
    handler.exportProperties(originator, propertiesByName);
    return new PropertyBagMemento(schemaId, schemaVersion, propertiesByName);
  }

  private static MementoExportException newUnsupportedSchemaVersionException(final long schemaVersion) {
    return new MementoExportException(String.format("schema version %d is unsupported", schemaVersion));
  }

  /**
   * A memento export handler.
   *
   * @param <T> The type of the memento originator.
   */
  @FunctionalInterface
  public interface Handler<T> {
    /**
     * Exports the properties of the specified originator to the specified collection.
     *
     * @param originator The memento originator; must not be {@code null}.
     * @param propertiesByName The collection that receives the originator properties; must not be {@code null}. The key
     *        is the property name. The value is the property value.
     *
     * @throws MementoExportException If an error occurs while exporting the originator properties.
     */
    void exportProperties(T originator, Map<String, Object> propertiesByName) throws MementoExportException;
  }

  /**
   * Supplies memento export handlers for each supported schema version.
   *
   * @param <T> The type of the memento originator.
   */
  @FunctionalInterface
  public interface HandlerSupplier<T> {
    /**
     * Gets the memento export handler for the specified schema version.
     *
     * @param schemaVersion The memento schema version to export.
     *
     * @return The memento export handler for the specified schema version or empty if no handler is available; never
     *         {@code null}.
     */
    Optional<Handler<T>> getHandler(long schemaVersion);
  }
}
