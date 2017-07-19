package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Map;

import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.memento.PropertyBagMemento;

/**
 * A serializable proxy for the {@link PropertyBagMemento} class.
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class PropertyBagMementoProxy implements Serializable {
  private static final long serialVersionUID = 7813364982800353383L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(PropertyBagMemento.class, PropertyBagMementoProxy::new);

  private final Map<String, Object> propertiesByName;
  private final String schemaId;

  public PropertyBagMementoProxy(final PropertyBagMemento memento) {
    checkNotNull(memento);

    propertiesByName = memento.getPropertiesByName();
    schemaId = memento.getSchemaId();
  }

  private Object readResolve() {
    return new PropertyBagMemento(schemaId, propertiesByName);
  }
}
