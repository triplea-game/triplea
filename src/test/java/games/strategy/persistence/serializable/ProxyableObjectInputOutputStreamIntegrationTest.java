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
    try (final InputStream is = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(is)) {
      return ois.readObject();
    }
  }

  private void writeObject(final Object obj, final ProxyRegistry proxyRegistry) throws Exception {
    try (final ObjectOutputStream oos = new ProxyableObjectOutputStream(baos, proxyRegistry)) {
      oos.writeObject(obj);
    }
  }

  @Test
  public void shouldBeAbleToRoundTripNull() throws Exception {
    final ProxyRegistry proxyRegistry = ProxyRegistry.newInstance();

    writeObject(null, proxyRegistry);
    final Object deserializedObj = readObject();

    assertThat(deserializedObj, is(nullValue()));
  }

  @Test
  public void shouldBeAbleToRoundTripSerializableObjectWithoutProxyFactory() throws Exception {
    final ProxyRegistry proxyRegistry = ProxyRegistry.newInstance();
    final Date obj = new Date();

    writeObject(obj, proxyRegistry);
    final Date deserializedObj = (Date) readObject();

    assertThat(deserializedObj, is(obj));
  }

  @Test
  public void shouldBeAbleToRoundTripNonSerializableObjectWithProxyFactory() throws Exception {
    final ProxyRegistry proxyRegistry = ProxyRegistry.newInstance(FakeNonSerializableClassProxy.FACTORY);
    final FakeNonSerializableClass obj = new FakeNonSerializableClass(2112, "42");

    writeObject(obj, proxyRegistry);
    final FakeNonSerializableClass deserializedObj = (FakeNonSerializableClass) readObject();

    assertThat(deserializedObj, is(obj));
  }

  @Test
  public void shouldThrowExceptionWhenWritingNonSerializableObjectWithNoRegisteredProxyFactory() throws Exception {
    final ProxyRegistry proxyRegistry = ProxyRegistry.newInstance();

    catchException(() -> writeObject(new FakeNonSerializableClass(2112, "42"), proxyRegistry));

    assertThat(caughtException(), is(instanceOf(NotSerializableException.class)));
  }
}
