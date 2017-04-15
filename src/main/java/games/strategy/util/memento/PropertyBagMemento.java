package games.strategy.util.memento;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link Memento} that represents the originator state as a property bag.
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class PropertyBagMemento implements Memento {
  private final Map<String, Object> propertiesByName;

  private final String schemaId;

  private final long schemaVersion;

  /**
   * Initializes a new instance of the {@code PropertyBagMemento} class.
   *
   * @param schemaId The memento schema identifier; must not be {@code null}.
   * @param schemaVersion The memento schema version.
   * @param propertiesByName The collection of originator properties; must not be {@code null}. The key is the property
   *        name. The value is the property value.
   */
  public PropertyBagMemento(
      final String schemaId,
      final long schemaVersion,
      final Map<String, Object> propertiesByName) {
    checkNotNull(schemaId);
    checkNotNull(propertiesByName);

    this.propertiesByName = new HashMap<>(propertiesByName);
    this.schemaId = schemaId;
    this.schemaVersion = schemaVersion;
  }

  /**
   * Gets the collection of originator properties.
   *
   * @return The collection of originator properties; never {@code null}. The key is the property name. The value is the
   *         property value.
   */
  public Map<String, Object> getPropertiesByName() {
    return new HashMap<>(propertiesByName);
  }

  /**
   * Gets the memento schema identifier.
   *
   * @return The memento schema identifier; never {@code null}.
   */
  public String getSchemaId() {
    return schemaId;
  }

  /**
   * Gets the memento schema version.
   *
   * @return The memento schema version.
   */
  public long getSchemaVersion() {
    return schemaVersion;
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
        && (schemaVersion == other.schemaVersion)
        && propertiesByName.equals(other.propertiesByName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schemaId, schemaVersion, propertiesByName);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("PropertyBagMemento[");
    sb.append("propertiesByName=");
    sb.append(propertiesByName);
    sb.append(", schemaId=");
    sb.append(schemaId);
    sb.append(", schemaVersion=");
    sb.append(schemaVersion);
    sb.append("]");
    return sb.toString();
  }
}
