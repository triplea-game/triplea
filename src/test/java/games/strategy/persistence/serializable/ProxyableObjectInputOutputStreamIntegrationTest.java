package games.strategy.persistence.serializable;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import org.junit.Test;

/**
 * A fixture for testing the integration between the {@link ObjectInputStream} and {@link ProxyableObjectOutputStream}
 * classes.
 */
public final class ProxyableObjectInputOutputStreamIntegrationTest {
  private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

  private Object readObject() throws Exception {
    return readObjects(1)[0];
  }

  private Object[] readObjects(final int count) throws Exception {
    final Object[] objs = new Object[count];
    try (final InputStream is = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(is)) {
      for (int i = 0; i < count; ++i) {
        objs[i] = ois.readObject();
      }
    }
    return objs;
  }

  private void writeObject(final ProxyRegistry proxyRegistry, final Object obj) throws Exception {
    writeObjects(proxyRegistry, obj);
  }

  private void writeObjects(final ProxyRegistry proxyRegistry, final Object... objs) throws Exception {
    try (final ObjectOutputStream oos = new ProxyableObjectOutputStream(baos, proxyRegistry)) {
      for (final Object obj : objs) {
        oos.writeObject(obj);
      }
    }
  }

  @Test
  public void shouldBeAbleToRoundTripNull() throws Exception {
    final ProxyRegistry proxyRegistry = ProxyRegistry.newInstance();

    writeObject(proxyRegistry, null);
    final Object deserializedObj = readObject();

    assertThat(deserializedObj, is(nullValue()));
  }

  @Test
  public void shouldBeAbleToRoundTripSerializableObjectWithoutProxyFactory() throws Exception {
    final ProxyRegistry proxyRegistry = ProxyRegistry.newInstance();
    final Date obj = new Date();

    writeObject(proxyRegistry, obj);
    final Date deserializedObj = (Date) readObject();

    assertThat(deserializedObj, is(obj));
  }

  @Test
  public void shouldPreserveReferencesToPreviouslySerializedSerializableObject() throws Exception {
    final ProxyRegistry proxyRegistry = ProxyRegistry.newInstance();
    final Date obj = new Date();

    writeObjects(proxyRegistry, obj, obj);
    final Object[] deserializedObjs = readObjects(2);
    final Date deserializedObj1 = (Date) deserializedObjs[0];
    final Date deserializedObj2 = (Date) deserializedObjs[1];

    assertThat(deserializedObj1, is(obj));
    assertThat(deserializedObj2, is(sameInstance(deserializedObj1)));
  }

  @Test
  public void shouldBeAbleToRoundTripNonSerializableObjectWithProxyFactory() throws Exception {
    final ProxyRegistry proxyRegistry = ProxyRegistry.newInstance(FakeNonSerializableClassProxy.FACTORY);
    final FakeNonSerializableClass obj = new FakeNonSerializableClass(2112, "42");

    writeObject(proxyRegistry, obj);
    final FakeNonSerializableClass deserializedObj = (FakeNonSerializableClass) readObject();

    assertThat(deserializedObj, is(obj));
  }

  @Test
  public void shouldPreserveReferencesToPreviouslySerializedNonSerializableObject() throws Exception {
    final ProxyRegistry proxyRegistry = ProxyRegistry.newInstance(FakeNonSerializableClassProxy.FACTORY);
    final FakeNonSerializableClass obj = new FakeNonSerializableClass(2112, "42");

    writeObjects(proxyRegistry, obj, obj);
    final Object[] deserializedObjs = readObjects(2);
    final FakeNonSerializableClass deserializedObj1 = (FakeNonSerializableClass) deserializedObjs[0];
    final FakeNonSerializableClass deserializedObj2 = (FakeNonSerializableClass) deserializedObjs[1];

    assertThat(deserializedObj1, is(obj));
    assertThat(deserializedObj2, is(sameInstance(deserializedObj1)));
  }

  @Test
  public void shouldThrowExceptionWhenWritingNonSerializableObjectWithNoRegisteredProxyFactory() throws Exception {
    final ProxyRegistry proxyRegistry = ProxyRegistry.newInstance();

    catchException(() -> writeObject(proxyRegistry, new FakeNonSerializableClass(2112, "42")));

    assertThat(caughtException(), is(instanceOf(NotSerializableException.class)));
  }
}
