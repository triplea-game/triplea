package games.strategy.internal.persistence.serializable;

import games.strategy.persistence.serializable.AbstractPersistenceDelegateTestCase;
import games.strategy.persistence.serializable.PersistenceDelegateRegistry;
import games.strategy.util.Version;

public final class VersionPersistenceDelegateAsPersistenceDelegateTest
    extends AbstractPersistenceDelegateTestCase<Version> {
  public VersionPersistenceDelegateAsPersistenceDelegateTest() {
    super(Version.class);
  }

  @Override
  protected Version createSubject() {
    return new Version(1, 2, 3, 4);
  }

  @Override
  protected void registerPersistenceDelegates(final PersistenceDelegateRegistry persistenceDelegateRegistry) {
    persistenceDelegateRegistry.registerPersistenceDelegate(Version.class, new VersionPersistenceDelegate());
  }
}
