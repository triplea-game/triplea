package games.strategy.persistence.serializable;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessage;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class VersionedProxySupportTest {
  @Mock
  private ObjectInput in;

  @Mock
  private ObjectOutput out;

  @Test
  public void read_ShouldCallCorrectProxyHandlerWhenVersionSupported() throws Exception {
    final AtomicBoolean wasV1Read = new AtomicBoolean(false);
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Reader(in -> {
      wasV1Read.set(true);
    });
    when(in.readLong()).thenReturn(1L);

    proxy.getVersionedProxySupport().read(in);

    assertThat(wasV1Read.get(), is(true));
  }

  private static FakeVersionedProxy newVersionedProxyWithV1Reader() {
    return newVersionedProxyWithV1Reader(in -> {
      // do nothing
    });
  }

  private static FakeVersionedProxy newVersionedProxyWithV1Reader(final Reader reader) {
    return new FakeVersionedProxy() {
      private static final long serialVersionUID = 7551665971441431048L;

      @SuppressWarnings("unused")
      private void readExternalV1(final ObjectInput in) throws IOException, ClassNotFoundException {
        reader.read(in);
      }
    };
  }

  @Test
  public void read_ShouldThrowExceptionWhenVersionUnsupported() throws Exception {
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Reader();
    when(in.readLong()).thenReturn(2L);

    catchException(() -> proxy.getVersionedProxySupport().read(in));

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("does not support version 2"))));
  }

  @Test
  public void read_ShouldThrowExceptionWhenProxyHandlerIsNotPrivate() throws Exception {
    final FakeVersionedProxy proxy = new FakeVersionedProxyWithNonPrivateV1Handlers();
    when(in.readLong()).thenReturn(1L);

    catchException(() -> proxy.getVersionedProxySupport().read(in));

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("does not support version 1"))));
  }

  @Test
  public void read_ShouldThrowExceptionWhenProxyHandlerIsStatic() throws Exception {
    final FakeVersionedProxy proxy = new FakeVersionedProxyWithStaticV1Handlers();
    when(in.readLong()).thenReturn(1L);

    catchException(() -> proxy.getVersionedProxySupport().read(in));

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("does not support version 1"))));
  }

  @Test
  public void read_ShouldThrowExceptionWhenProxyHandlerDoesNotReturnVoid() throws Exception {
    final FakeVersionedProxy proxy = new FakeVersionedProxyWithNonVoidReturnTypeV1Handlers();
    when(in.readLong()).thenReturn(1L);

    catchException(() -> proxy.getVersionedProxySupport().read(in));

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("does not support version 1"))));
  }

  @Test
  public void read_ShouldRethrowExceptionWhenProxyHandlerThrowsIoException() throws Exception {
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Reader(in -> {
      throw new IOException("42");
    });
    when(in.readLong()).thenReturn(1L);

    catchException(() -> proxy.getVersionedProxySupport().read(in));

    assertThat(caughtException(), allOf(is(instanceOf(IOException.class)), hasMessage("42")));
  }

  @Test
  public void read_ShouldRethrowExceptionWhenProxyHandlerThrowsClassNotFoundException() throws Exception {
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Reader(in -> {
      throw new ClassNotFoundException("42");
    });
    when(in.readLong()).thenReturn(1L);

    catchException(() -> proxy.getVersionedProxySupport().read(in));

    assertThat(caughtException(), allOf(is(instanceOf(ClassNotFoundException.class)), hasMessage("42")));
  }

  @Test
  public void read_ShouldRethrowExceptionWhenProxyHandlerThrowsRuntimeException() throws Exception {
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Reader(in -> {
      throw new RuntimeException("42");
    });
    when(in.readLong()).thenReturn(1L);

    catchException(() -> proxy.getVersionedProxySupport().read(in));

    assertThat(caughtException(), allOf(is(instanceOf(RuntimeException.class)), hasMessage("42")));
  }

  @Test
  public void write_ShouldCallCorrectProxyHandlerWhenVersionSupported() throws Exception {
    final AtomicBoolean wasV1Written = new AtomicBoolean(false);
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Writer(out -> {
      wasV1Written.set(true);
    });

    proxy.getVersionedProxySupport().write(out, 1);

    assertThat(wasV1Written.get(), is(true));
  }

  private static FakeVersionedProxy newVersionedProxyWithV1Writer() {
    return newVersionedProxyWithV1Writer(in -> {
      // do nothing
    });
  }

  private static FakeVersionedProxy newVersionedProxyWithV1Writer(final Writer writer) {
    return new FakeVersionedProxy() {
      private static final long serialVersionUID = 5191953634904657494L;

      @SuppressWarnings("unused")
      private void writeExternalV1(final ObjectOutput out) throws IOException {
        writer.write(out);
      }
    };
  }

  @Test
  public void write_ShouldThrowExceptionWhenVersionUnsupported() throws Exception {
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Writer();

    catchException(() -> proxy.getVersionedProxySupport().write(out, 2));

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("does not support version 2"))));
  }

  @Test
  public void write_ShouldThrowExceptionWhenProxyHandlerIsNotPrivate() throws Exception {
    final FakeVersionedProxy proxy = new FakeVersionedProxyWithNonPrivateV1Handlers();

    catchException(() -> proxy.getVersionedProxySupport().write(out, 1));

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("does not support version 1"))));
  }

  @Test
  public void write_ShouldThrowExceptionWhenProxyHandlerIsStatic() throws Exception {
    final FakeVersionedProxy proxy = new FakeVersionedProxyWithStaticV1Handlers();

    catchException(() -> proxy.getVersionedProxySupport().write(out, 1));

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("does not support version 1"))));
  }

  @Test
  public void write_ShouldThrowExceptionWhenProxyHandlerDoesNotReturnVoid() throws Exception {
    final FakeVersionedProxy proxy = new FakeVersionedProxyWithNonVoidReturnTypeV1Handlers();

    catchException(() -> proxy.getVersionedProxySupport().write(out, 1));

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("does not support version 1"))));
  }

  @Test
  public void write_ShouldRethrowExceptionWhenProxyHandlerThrowsIoException() throws Exception {
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Writer(out -> {
      throw new IOException("42");
    });

    catchException(() -> proxy.getVersionedProxySupport().write(out, 1));

    assertThat(caughtException(), allOf(is(instanceOf(IOException.class)), hasMessage("42")));
  }

  @Test
  public void write_ShouldRethrowExceptionWhenProxyHandlerThrowsRuntimeException() throws Exception {
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Writer(out -> {
      throw new RuntimeException("42");
    });

    catchException(() -> proxy.getVersionedProxySupport().write(out, 1));

    assertThat(caughtException(), allOf(is(instanceOf(RuntimeException.class)), hasMessage("42")));
  }

  @Test
  public void write_ShouldThrowExceptionWhenVersionIsZero() {
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Writer();

    catchException(() -> proxy.getVersionedProxySupport().write(out, 0));

    assertThat(caughtException(), is(instanceOf(IllegalArgumentException.class)));
  }

  @Test
  public void write_ShouldThrowExceptionWhenVersionIsNegative() {
    final FakeVersionedProxy proxy = newVersionedProxyWithV1Writer();

    catchException(() -> proxy.getVersionedProxySupport().write(out, -1));

    assertThat(caughtException(), is(instanceOf(IllegalArgumentException.class)));
  }

  @FunctionalInterface
  private interface Reader {
    void read(ObjectInput in) throws IOException, ClassNotFoundException;
  }

  @FunctionalInterface
  private interface Writer {
    void write(ObjectOutput out) throws IOException;
  }

  private static class FakeVersionedProxy implements Externalizable {
    private static final long serialVersionUID = 4915747298980371948L;

    private final VersionedProxySupport versionedProxySupport = new VersionedProxySupport(this);

    final VersionedProxySupport getVersionedProxySupport() {
      return versionedProxySupport;
    }

    @Override
    public final void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
      versionedProxySupport.read(in);
    }

    @Override
    public final void writeExternal(final ObjectOutput out) throws IOException {
      versionedProxySupport.write(out, 1);
    }
  }

  private static final class FakeVersionedProxyWithNonPrivateV1Handlers extends FakeVersionedProxy {
    private static final long serialVersionUID = 3580688683441635324L;

    @SuppressWarnings("unused")
    public void readExternalV1(final ObjectInput in) {
      // do nothing
    }

    @SuppressWarnings("unused")
    public void writeExternalV1(final ObjectOutput out) {
      // do nothing
    }
  }

  private static final class FakeVersionedProxyWithStaticV1Handlers extends FakeVersionedProxy {
    private static final long serialVersionUID = 1432485788155460084L;

    @SuppressWarnings("unused")
    private static void readExternalV1(final ObjectInput in) {
      // do nothing
    }

    @SuppressWarnings("unused")
    private static void writeExternalV1(final ObjectOutput out) {
      // do nothing
    }
  }

  private static final class FakeVersionedProxyWithNonVoidReturnTypeV1Handlers extends FakeVersionedProxy {
    private static final long serialVersionUID = -5845503130403910828L;

    @SuppressWarnings("unused")
    private Object readExternalV1(final ObjectInput in) {
      return this;
    }

    @SuppressWarnings("unused")
    private Object writeExternalV1(final ObjectOutput out) {
      return this;
    }
  }
}
