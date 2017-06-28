package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Map;

import games.strategy.util.memento.PropertyBagMemento;

public final class PropertyBagMementoProxy implements Serializable {
  private static final long serialVersionUID = 7813364982800353383L;

  /**
   * @serial The collection of originator properties; never {@code null}. The key is the property name. The value is the
   *         property value.
   */
  private final Map<String, Object> propertiesByName;

  /**
   * @serial The memento schema identifier; never {@code null}.
   */
  private final String schemaId;

  /**
   * @serial The memento schema version.
   */
  private final long schemaVersion;

  /**
   * Initializes a new instance of the {@code PropertyBagMementoProxy} class from the specified
   * {@code PropertyBagMemento} instance.
   *
   * @param memento The {@code PropertyBagMemento} instance; must not be {@code null}.
   */
  public PropertyBagMementoProxy(final PropertyBagMemento memento) {
    checkNotNull(memento);

    propertiesByName = memento.getPropertiesByName();
    schemaId = memento.getSchemaId();
    schemaVersion = memento.getSchemaVersion();
  }

  private Object readResolve() {
    return new PropertyBagMemento(schemaId, schemaVersion, propertiesByName);
  }
}
