package games.strategy.internal.persistence.serializable;

import java.io.IOException;

import games.strategy.persistence.serializable.AbstractPersistenceDelegate;
import games.strategy.util.memento.PropertyBagMemento;

/**
 * A persistence delegate for the {@link PropertyBagMemento} class.
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class PropertyBagMementoPersistenceDelegate extends AbstractPersistenceDelegate {
  @Override
  public Object replaceObject(final Object obj) throws IOException {
    return (obj instanceof PropertyBagMemento)
        ? new PropertyBagMementoProxy((PropertyBagMemento) obj)
        : super.replaceObject(obj);
  }
}
