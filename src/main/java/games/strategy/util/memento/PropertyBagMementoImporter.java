package games.strategy.util.memento;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

/**
 * Implementation of {@link MementoImporter} for instances of {@link PropertyBagMemento}.
 *
 * @param <T> The type of the memento originator.
 */
public final class PropertyBagMementoImporter<T> implements MementoImporter<T> {
  private final Handler<T> handler;

  private final String schemaId;

  /**
   * Initializes a new instance of the {@code PropertyBagMementoImporter} class.
   *
   * @param schemaId The schema identifier of the memento to import.
   * @param handler The import handler for the schema.
   */
  public PropertyBagMementoImporter(final String schemaId, final Handler<T> handler) {
    checkNotNull(schemaId);
    checkNotNull(handler);

    this.handler = handler;
    this.schemaId = schemaId;
  }

  @Override
  public T importMemento(final Memento memento) throws MementoImportException {
    checkNotNull(memento);

    if (!(memento instanceof PropertyBagMemento)) {
      throw newUnsupportedMementoException(memento);
    }

    return importMemento((PropertyBagMemento) memento);
  }

  /**
   * Imports the specified memento and returns the originator.
   *
   * @param memento The memento to be imported.
   *
   * @return The memento originator.
   *
   * @throws MementoImportException If an error occurs while importing the memento.
   */
  public T importMemento(final PropertyBagMemento memento) throws MementoImportException {
    checkNotNull(memento);

    if (!schemaId.equals(memento.getSchemaId())) {
      throw newUnsupportedSchemaIdException(memento.getSchemaId());
    }

    return handler.importProperties(memento.getPropertiesByName());
  }

  private static MementoImportException newUnsupportedMementoException(final Memento memento) {
    return new MementoImportException(String.format("memento has wrong type: expected '%s' but was '%s'",
        PropertyBagMemento.class.getName(), memento.getClass().getName()));
  }

  private static MementoImportException newUnsupportedSchemaIdException(final String schemaId) {
    return new MementoImportException(String.format("schema ID '%s' is unsupported", schemaId));
  }

  /**
   * A memento import handler.
   *
   * @param <T> The type of the memento originator.
   */
  @FunctionalInterface
  public interface Handler<T> {
    /**
     * Imports the specified originator properties and creates a new originator instance.
     *
     * @param propertiesByName The collection of originator properties. The key is the property name. The value is the
     *        property value.
     *
     * @return A new originator instance created from the properties.
     *
     * @throws MementoImportException If an error occurs while importing the originator properties.
     */
    T importProperties(Map<String, Object> propertiesByName) throws MementoImportException;
  }
}
