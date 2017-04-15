package games.strategy.persistence.serializable;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * A fixture for testing the basic aspects of classes that implement the {@link PersistenceDelegateRegistry} interface.
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractPersistenceDelegateRegistryTestCase {
  @Mock
  private PersistenceDelegate persistenceDelegate;

  private PersistenceDelegateRegistry persistenceDelegateRegistry;

  private final Class<?> type = Object.class;

  /**
   * Initializes a new instance of the {@code AbstractPersistenceDelegateRegistryTestCase} class.
   */
  protected AbstractPersistenceDelegateRegistryTestCase() {}

  /**
   * Creates the persistence delegate registry to be tested.
   *
   * @return The persistence delegate registry to be tested; never {@code null}.
   *
   * @throws Exception If the persistence delegate registry cannot be created.
   */
  protected abstract PersistenceDelegateRegistry createPersistenceDelegateRegistry() throws Exception;

  /**
   * Gets the persistence delegate registry under test in the fixture.
   *
   * @return The persistence delegate registry under test in the fixture; never {@code null}.
   */
  protected final PersistenceDelegateRegistry getPersistenceDelegateRegistry() {
    assert persistenceDelegateRegistry != null;
    return persistenceDelegateRegistry;
  }

  /**
   * Sets up the test fixture.
   *
   * <p>
   * Subclasses may override and must call the superclass implementation.
   * </p>
   *
   * @throws Exception If an error occurs.
   */
  @Before
  public void setUp() throws Exception {
    persistenceDelegateRegistry = createPersistenceDelegateRegistry();
    assert persistenceDelegateRegistry != null;
  }

  @Test
  public void getPersistenceDelegateForType_ShouldReturnPersistenceDelegateWhenTypePresent() {
    persistenceDelegateRegistry.registerPersistenceDelegate(type, persistenceDelegate);

    assertThat(persistenceDelegateRegistry.getPersistenceDelegate(type), is(Optional.of(persistenceDelegate)));
  }

  @Test
  public void getPersistenceDelegateForType_ShouldReturnEmptyWhenTypeAbsent() {
    assertThat(persistenceDelegateRegistry.getPersistenceDelegate(type), is(Optional.empty()));
  }

  @Test
  public void getPersistenceDelegateForTypeName_ShouldReturnPersistenceDelegateWhenTypeNamePresent() {
    persistenceDelegateRegistry.registerPersistenceDelegate(type, persistenceDelegate);

    assertThat(persistenceDelegateRegistry.getPersistenceDelegate(type.getName()),
        is(Optional.of(persistenceDelegate)));
  }

  @Test
  public void getPersistenceDelegateForTypeName_ShouldReturnEmptyWhenTypeNameAbsent() {
    assertThat(persistenceDelegateRegistry.getPersistenceDelegate("unknownTypeName"), is(Optional.empty()));
  }

  @Test
  public void getTypeNames_ShouldReturnCopy() {
    final Set<String> typeNames = persistenceDelegateRegistry.getTypeNames();
    final int expectedTypeNamesSize = typeNames.size();

    typeNames.add(type.getName());

    assertThat(persistenceDelegateRegistry.getTypeNames(), hasSize(expectedTypeNamesSize));
  }

  @Test
  public void getTypeNames_ShouldReturnSnapshot() {
    final Set<String> typeNames = persistenceDelegateRegistry.getTypeNames();
    persistenceDelegateRegistry.registerPersistenceDelegate(type, persistenceDelegate);

    assertThat(persistenceDelegateRegistry.getTypeNames(), is(not(typeNames)));
  }

  @Test
  public void registerPersistenceDelegate_ShouldRegisterPersistenceDelegateWhenTypeUnregistered() {
    persistenceDelegateRegistry.registerPersistenceDelegate(type, persistenceDelegate);

    assertThat(persistenceDelegateRegistry.getPersistenceDelegate(type), is(Optional.of(persistenceDelegate)));
  }

  @Test
  public void registerPersistenceDelegate_ShouldThrowExceptionWhenTypeRegistered() {
    persistenceDelegateRegistry.registerPersistenceDelegate(type, persistenceDelegate);

    final PersistenceDelegate otherPersistenceDelegate = mock(PersistenceDelegate.class);
    catchException(() -> persistenceDelegateRegistry.registerPersistenceDelegate(type, otherPersistenceDelegate));

    assertThat(caughtException(), is(instanceOf(IllegalArgumentException.class)));
  }

  @Test
  public void unregisterPersistenceDelegate_ShouldUnregisterPersistenceDelegateWhenTypeRegisteredWithSameInstance() {
    final int originalTypeNamesSize = persistenceDelegateRegistry.getTypeNames().size();
    persistenceDelegateRegistry.registerPersistenceDelegate(type, persistenceDelegate);
    assertThat(persistenceDelegateRegistry.getTypeNames(), hasSize(originalTypeNamesSize + 1));

    persistenceDelegateRegistry.unregisterPersistenceDelegate(type, persistenceDelegate);

    assertThat(persistenceDelegateRegistry.getPersistenceDelegate(type), is(Optional.empty()));
    assertThat(persistenceDelegateRegistry.getTypeNames(), hasSize(originalTypeNamesSize));
  }

  @Test
  public void unregisterPersistenceDelegate_ShouldThrowExceptionWhenTypeUnregistered() {
    catchException(() -> persistenceDelegateRegistry.unregisterPersistenceDelegate(type, persistenceDelegate));

    assertThat(caughtException(), is(instanceOf(IllegalArgumentException.class)));
  }

  @Test
  public void unregisterPersistenceDelegate_ShouldThrowExceptionWhenTypeRegisteredWithDifferentInstance() {
    persistenceDelegateRegistry.registerPersistenceDelegate(type, persistenceDelegate);

    final PersistenceDelegate otherPersistenceDelegate = mock(PersistenceDelegate.class);
    catchException(() -> persistenceDelegateRegistry.unregisterPersistenceDelegate(type, otherPersistenceDelegate));

    assertThat(caughtException(), is(instanceOf(IllegalArgumentException.class)));
  }
}
