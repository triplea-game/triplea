package games.strategy.persistence.serializable;

public final class DefaultPersistenceDelegateRegistryAsPersistenceDelegateRegistryTest
    extends AbstractPersistenceDelegateRegistryTestCase {
  @Override
  protected PersistenceDelegateRegistry createPersistenceDelegateRegistry() {
    return new DefaultPersistenceDelegateRegistry();
  }
}
