package games.strategy.persistence.serializable;

public final class FakeNonSerializableClassPersistenceDelegateAsPersistenceDelegateTest
    extends AbstractPersistenceDelegateTestCase<FakeNonSerializableClass> {
  public FakeNonSerializableClassPersistenceDelegateAsPersistenceDelegateTest() {
    super(FakeNonSerializableClass.class);
  }

  @Override
  protected FakeNonSerializableClass createSubject() {
    return new FakeNonSerializableClass(2112, "42");
  }

  @Override
  protected void registerPersistenceDelegates(final PersistenceDelegateRegistry persistenceDelegateRegistry) {
    persistenceDelegateRegistry.registerPersistenceDelegate(
        FakeNonSerializableClass.class, new FakeNonSerializableClassPersistenceDelegate());
  }
}
