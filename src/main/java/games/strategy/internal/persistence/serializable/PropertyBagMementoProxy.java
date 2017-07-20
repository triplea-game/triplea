package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.memento.PropertyBagMemento;
import net.jcip.annotations.Immutable;

/**
 * A serializable proxy for the {@link PropertyBagMemento} class.
 */
@Immutable
public final class PropertyBagMementoProxy implements Proxy {
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

  @Override
  public Object readResolve() {
    return new PropertyBagMemento(schemaId, propertiesByName);
  }
}
