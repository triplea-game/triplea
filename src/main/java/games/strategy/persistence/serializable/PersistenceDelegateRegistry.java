package games.strategy.persistence.serializable;

import java.util.Optional;
import java.util.Set;

/**
 * A service for the management and discovery of Java object serialization framework persistence delegates.
 */
public interface PersistenceDelegateRegistry {
  /**
   * Gets the persistence delegate associated with the specified type.
   *
   * @param type The type associated with the persistence delegate; must not be {@code null}.
   *
   * @return The persistence delegate associated with the specified type or empty if no such type is registered.
   */
  Optional<PersistenceDelegate> getPersistenceDelegate(Class<?> type);

  /**
   * Gets the persistence delegate associated with the specified type name.
   *
   * @param typeName The name of the type associated with the persistence delegate; must not be {@code null}.
   *
   * @return The persistence delegate associated with the specified type name or empty if no such type name is
   *         registered.
   */
  Optional<PersistenceDelegate> getPersistenceDelegate(String typeName);

  /**
   * Gets a collection of all type names for which a persistence delegate has been registered with this service.
   *
   * @return A collection of all type names for which a persistence delegate has been registered with this service;
   *         never {@code null}. This collection is a snapshot of the persistence delegates registered at the time of
   *         the call.
   */
  Set<String> getTypeNames();

  /**
   * Registers the specified persistence delegate for the specified type.
   *
   * @param type The type associated with the persistence delegate; must not be {@code null}.
   * @param persistenceDelegate The persistence delegate; must not be {@code null}.
   *
   * @throws IllegalArgumentException If a persistence delegate is already registered for the specified type.
   */
  void registerPersistenceDelegate(Class<?> type, PersistenceDelegate persistenceDelegate);

  /**
   * Unregisters the persistence delegate for the specified type.
   *
   * @param type The type associated with the persistence delegate; must not be {@code null}.
   * @param persistenceDelegate The persistence delegate; must not be {@code null}.
   *
   * @throws IllegalArgumentException If the specified persistence delegate was not previously registered for the
   *         specified type.
   */
  void unregisterPersistenceDelegate(Class<?> type, PersistenceDelegate persistenceDelegate);
}
