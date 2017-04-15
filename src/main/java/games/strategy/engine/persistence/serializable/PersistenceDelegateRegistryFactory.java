package games.strategy.engine.persistence.serializable;

import games.strategy.internal.persistence.serializable.PropertyBagMementoPersistenceDelegate;
import games.strategy.internal.persistence.serializable.VersionPersistenceDelegate;
import games.strategy.persistence.serializable.DefaultPersistenceDelegateRegistry;
import games.strategy.persistence.serializable.PersistenceDelegateRegistry;
import games.strategy.util.Version;
import games.strategy.util.memento.PropertyBagMemento;

/**
 * Factory for creating pre-configured instances of {@link PersistenceDelegateFactory}.
 */
public final class PersistenceDelegateRegistryFactory {
  private PersistenceDelegateRegistryFactory() {}

  /**
   * Creates a new {@link PersistenceDelegateRegistry} that has been configured with all platform persistence delegates.
   *
   * @return A new {@link PersistenceDelegateRegistry}; never {@code null}.
   */
  public static PersistenceDelegateRegistry newPlatformPersistenceDelegateRegistry() {
    final PersistenceDelegateRegistry persistenceDelegateRegistry = new DefaultPersistenceDelegateRegistry();
    persistenceDelegateRegistry.registerPersistenceDelegate(
        PropertyBagMemento.class, new PropertyBagMementoPersistenceDelegate());
    persistenceDelegateRegistry.registerPersistenceDelegate(Version.class, new VersionPersistenceDelegate());
    return persistenceDelegateRegistry;
  }
}
