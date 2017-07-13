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
import java.util.Date;

import org.junit.Test;

/**
 * A fixture for testing the integration between the {@link ObjectInputStream} and {@link ObjectOutputStream} classes.
 */
public final class ObjectInputOutputStreamIntegrationTest {
  private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

  private Object readObject() throws Exception {
    try (final InputStream is = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(is)) {
      return ois.readObject();
    }
  }

  private void writeObject(final Object obj, final ProxyFactoryRegistry proxyFactoryRegistry) throws Exception {
    try (final ObjectOutputStream oos = new ObjectOutputStream(baos, proxyFactoryRegistry)) {
      oos.writeObject(obj);
    }
  }

  @Test
  public void shouldBeAbleToRoundTripNull() throws Exception {
    final ProxyFactoryRegistry proxyFactoryRegistry = ProxyFactoryRegistry.newInstance();

    writeObject(null, proxyFactoryRegistry);
    final Object deserializedObj = readObject();

    assertThat(deserializedObj, is(nullValue()));
  }

  @Test
  public void shouldBeAbleToRoundTripSerializableObjectWithoutProxyFactory() throws Exception {
    final ProxyFactoryRegistry proxyFactoryRegistry = ProxyFactoryRegistry.newInstance();
    final Date obj = new Date();

    writeObject(obj, proxyFactoryRegistry);
    final Date deserializedObj = (Date) readObject();

    assertThat(deserializedObj, is(obj));
  }

  @Test
  public void shouldBeAbleToRoundTripNonSerializableObjectWithProxyFactory() throws Exception {
    final ProxyFactoryRegistry proxyFactoryRegistry = ProxyFactoryRegistry.newInstance(
        FakeNonSerializableClassProxy.FACTORY);
    final FakeNonSerializableClass obj = new FakeNonSerializableClass(2112, "42");

    writeObject(obj, proxyFactoryRegistry);
    final FakeNonSerializableClass deserializedObj = (FakeNonSerializableClass) readObject();

    assertThat(deserializedObj, is(obj));
  }

  @Test
  public void shouldThrowExceptionWhenWritingNonSerializableObjectWithNoRegisteredProxyFactory() throws Exception {
    final ProxyFactoryRegistry proxyFactoryRegistry = ProxyFactoryRegistry.newInstance();

    catchException(() -> writeObject(new FakeNonSerializableClass(2112, "42"), proxyFactoryRegistry));

    assertThat(caughtException(), is(instanceOf(NotSerializableException.class)));
  }
}
