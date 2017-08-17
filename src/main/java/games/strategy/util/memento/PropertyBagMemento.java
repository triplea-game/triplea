package games.strategy.util.memento;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.concurrent.Immutable;

/**
 * Implementation of {@link Memento} that represents the originator state as a collection of name-value pairs.
 */
@Immutable
public final class PropertyBagMemento implements Memento {
  private final Map<String, Object> propertiesByName;

  private final String schemaId;

  public PropertyBagMemento(final String schemaId, final Map<String, Object> propertiesByName) {
    checkNotNull(schemaId);
    checkNotNull(propertiesByName);

    this.propertiesByName = new HashMap<>(propertiesByName);
    this.schemaId = schemaId;
  }

  /**
   * Gets the collection of originator properties.
   *
   * @return The collection of originator properties. The key is the property name. The value is the property value.
   */
  public Map<String, Object> getPropertiesByName() {
    return new HashMap<>(propertiesByName);
  }

  /**
   * Gets the memento schema identifier.
   *
   * @return The memento schema identifier.
   */
  public String getSchemaId() {
    return schemaId;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof PropertyBagMemento)) {
      return false;
    }

    final PropertyBagMemento other = (PropertyBagMemento) obj;
    return schemaId.equals(other.schemaId)
        && propertiesByName.equals(other.propertiesByName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schemaId, propertiesByName);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("PropertyBagMemento[");
    sb.append("propertiesByName=");
    sb.append(propertiesByName);
    sb.append(", schemaId=");
    sb.append(schemaId);
    sb.append("]");
    return sb.toString();
  }
}
