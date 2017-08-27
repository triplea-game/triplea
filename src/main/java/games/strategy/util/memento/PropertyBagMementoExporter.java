package games.strategy.util.memento;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link MementoExporter} for instances of {@link PropertyBagMemento}.
 *
 * @param <T> The type of the memento originator.
 */
public final class PropertyBagMementoExporter<T> implements MementoExporter<T> {
  private final Handler<T> handler;

  private final String schemaId;

  public PropertyBagMementoExporter(final String schemaId, final Handler<T> handler) {
    checkNotNull(schemaId);
    checkNotNull(handler);

    this.handler = handler;
    this.schemaId = schemaId;
  }

  @Override
  public PropertyBagMemento exportMemento(final T originator) throws MementoExportException {
    checkNotNull(originator);

    final Map<String, Object> propertiesByName = new HashMap<>();
    handler.exportProperties(originator, propertiesByName);
    return new PropertyBagMemento(schemaId, propertiesByName);
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
     * @param originator The memento originator.
     * @param propertiesByName The collection that receives the originator properties. The key is the property name. The
     *        value is the property value.
     *
     * @throws MementoExportException If an error occurs while exporting the originator properties.
     */
    void exportProperties(T originator, Map<String, Object> propertiesByName) throws MementoExportException;
  }
}
