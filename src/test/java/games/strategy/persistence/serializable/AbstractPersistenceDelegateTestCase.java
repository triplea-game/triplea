package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

/**
 * A fixture for testing the basic aspects of classes that implement the {@link PersistenceDelegate} interface.
 *
 * @param <T> The type of the subject to be persisted.
 */
public abstract class AbstractPersistenceDelegateTestCase<T> {
  private final PersistenceDelegateRegistry persistenceDelegateRegistry = new DefaultPersistenceDelegateRegistry();

  private final Class<T> type;

  /**
   * Initializes a new instance of the {@code AbstractPersistenceDelegateTestCase} class.
   *
   * @param type The type of the subject to be persisted; must not be {@code null}.
   */
  protected AbstractPersistenceDelegateTestCase(final Class<T> type) {
    checkNotNull(type);

    this.type = type;
  }

  /**
   * Asserts that the specified subjects are equal.
   *
   * <p>
   * This implementation compares the two objects using the {@code equals} method.
   * </p>
   *
   * @param expected The expected subject; must not be {@code null}.
   * @param actual The actual subject; must not be {@code null}.
   *
   * @throws AssertionError If the two subjects are not equal.
   */
  protected void assertSubjectEquals(final T expected, final T actual) {
    checkNotNull(expected);
    checkNotNull(actual);

    assertThat(actual, is(expected));
  }

  /**
   * Creates the subject to be persisted.
   *
   * @return The subject to be persisted; never {@code null}.
   */
  protected abstract T createSubject();

  /**
   * Registers the persistence delegates required for the subject to be persisted.
   *
   * @param persistenceDelegateRegistry The persistence delegate registry for use in the fixture; must not be
   *        {@code null}.
   */
  protected abstract void registerPersistenceDelegates(PersistenceDelegateRegistry persistenceDelegateRegistry);

  private Object readObject(final ByteArrayOutputStream baos) throws Exception {
    try (final InputStream is = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(is, persistenceDelegateRegistry)) {
      return ois.readObject();
    }
  }

  private void writeObject(final ByteArrayOutputStream baos, final T obj) throws Exception {
    try (final ObjectOutputStream oos = new ObjectOutputStream(baos, persistenceDelegateRegistry)) {
      oos.writeObject(obj);
    }
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
    registerPersistenceDelegates(persistenceDelegateRegistry);
  }

  @Test
  public void persistenceDelegate_ShouldBeAbleToRoundTripSubject() throws Exception {
    final T expected = createSubject();
    assertThat(expected, is(not(nullValue())));
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    writeObject(baos, expected);
    final Object untypedActual = readObject(baos);

    assertThat(untypedActual, is(not(nullValue())));
    assertThat(untypedActual, is(instanceOf(type)));
    final T actual = type.cast(untypedActual);
    assertSubjectEquals(expected, actual);
  }
}
