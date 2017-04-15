package games.strategy.persistence.serializable;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.util.Date;

import org.junit.Test;

/**
 * A fixture for testing the integration between the {@link ObjectInputStream} and {@link ObjectOutputStream} classes.
 */
public final class ObjectInputOutputStreamIntegrationTest {
  private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

  private final PersistenceDelegateRegistry persistenceDelegateRegistry = new DefaultPersistenceDelegateRegistry();

  private Object readObject() throws Exception {
    try (final InputStream is = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(is, persistenceDelegateRegistry)) {
      return ois.readObject();
    }
  }

  private void writeObject(final Object obj) throws Exception {
    try (final ObjectOutputStream oos = new ObjectOutputStream(baos, persistenceDelegateRegistry)) {
      oos.writeObject(obj);
    }
  }

  @Test
  public void shouldBeAbleToRoundTripNull() throws Exception {
    writeObject(null);
    final Object deserializedObj = readObject();

    assertThat(deserializedObj, is(nullValue()));
  }

  @Test
  public void shouldBeAbleToRoundTripSerializableObjectWithoutPersistenceDelegate()
      throws Exception {
    final Date obj = new Date();

    writeObject(obj);
    final Date deserializedObj = (Date) readObject();

    assertThat(deserializedObj, is(obj));
  }

  @Test
  public void shouldBeAbleToRoundTripNonSerializableObjectWithPersistenceDelegate()
      throws Exception {
    persistenceDelegateRegistry.registerPersistenceDelegate(
        FakeNonSerializableClass.class, new FakeNonSerializableClassPersistenceDelegate());
    final FakeNonSerializableClass obj = new FakeNonSerializableClass(2112, "42");

    writeObject(obj);
    final FakeNonSerializableClass deserializedObj = (FakeNonSerializableClass) readObject();

    assertThat(deserializedObj, is(obj));
  }

  @Test
  public void shouldThrowExceptionWhenWritingNonSerializableObjectWithNoRegisteredPersistenceDelegate()
      throws Exception {
    catchException(() -> writeObject(new FakeNonSerializableClass(2112, "42")));

    assertThat(caughtException(), is(instanceOf(NotSerializableException.class)));
  }

  @Test
  public void shouldNotThrowExceptionWhenReadingNonSerializableObjectWithNoRegisteredPersistenceDelegate()
      throws Exception {
    final PersistenceDelegate persistenceDelegate = new FakeNonSerializableClassPersistenceDelegate();
    persistenceDelegateRegistry.registerPersistenceDelegate(FakeNonSerializableClass.class, persistenceDelegate);
    final FakeNonSerializableClass obj = new FakeNonSerializableClass(2112, "42");

    writeObject(obj);
    persistenceDelegateRegistry.unregisterPersistenceDelegate(FakeNonSerializableClass.class, persistenceDelegate);
    final FakeNonSerializableClass deserializedObj = (FakeNonSerializableClass) readObject();

    assertThat(deserializedObj, is(obj));
  }
}
