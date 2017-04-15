package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of {@link PersistenceDelegateRegistry}.
 *
 * <p>
 * Instances of this class are not thread safe.
 * </p>
 */
public final class DefaultPersistenceDelegateRegistry implements PersistenceDelegateRegistry {
  private final Map<String, PersistenceDelegate> persistenceDelegatesByTypeName = new HashMap<>();

  @Override
  public Optional<PersistenceDelegate> getPersistenceDelegate(final Class<?> type) {
    checkNotNull(type);

    return getPersistenceDelegate(type.getName());
  }

  @Override
  public Optional<PersistenceDelegate> getPersistenceDelegate(final String typeName) {
    checkNotNull(typeName);

    return Optional.ofNullable(persistenceDelegatesByTypeName.get(typeName));
  }

  @Override
  public Set<String> getTypeNames() {
    return new HashSet<>(persistenceDelegatesByTypeName.keySet());
  }

  @Override
  public void registerPersistenceDelegate(final Class<?> type, final PersistenceDelegate persistenceDelegate) {
    checkNotNull(type);
    checkNotNull(persistenceDelegate);
    checkArgument(
        persistenceDelegatesByTypeName.putIfAbsent(type.getName(), persistenceDelegate) == null,
        "type is already registered");
  }

  @Override
  public void unregisterPersistenceDelegate(final Class<?> type, final PersistenceDelegate persistenceDelegate) {
    checkNotNull(type);
    checkNotNull(persistenceDelegate);
    checkArgument(
        persistenceDelegatesByTypeName.remove(type.getName(), persistenceDelegate),
        "type is not registered or was registered with a different persistence delegate");
  }
}
