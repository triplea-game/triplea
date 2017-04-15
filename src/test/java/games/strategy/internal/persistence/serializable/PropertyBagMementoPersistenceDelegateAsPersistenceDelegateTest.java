package games.strategy.internal.persistence.serializable;

import com.google.common.collect.ImmutableMap;

import games.strategy.persistence.serializable.AbstractPersistenceDelegateTestCase;
import games.strategy.persistence.serializable.PersistenceDelegateRegistry;
import games.strategy.util.memento.PropertyBagMemento;

public final class PropertyBagMementoPersistenceDelegateAsPersistenceDelegateTest
    extends AbstractPersistenceDelegateTestCase<PropertyBagMemento> {
  public PropertyBagMementoPersistenceDelegateAsPersistenceDelegateTest() {
    super(PropertyBagMemento.class);
  }

  @Override
  protected PropertyBagMemento createSubject() {
    return new PropertyBagMemento("schema-id", 8L, ImmutableMap.<String, Object>of(
        "property1", 42L,
        "property2", "2112"));
  }

  @Override
  protected void registerPersistenceDelegates(final PersistenceDelegateRegistry persistenceDelegateRegistry) {
    persistenceDelegateRegistry.registerPersistenceDelegate(
        PropertyBagMemento.class, new PropertyBagMementoPersistenceDelegate());
  }
}
