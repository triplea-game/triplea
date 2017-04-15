package games.strategy.persistence.serializable;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import org.junit.Test;

/**
 * A fixture for testing the ability of a serializable proxy that uses {@link VersionedProxySupport} to read and write
 * multiple versions of its serialized form.
 */
public final class VersionedProxyTest {
  /**
   * Tests that a version 2 proxy can read the serialized form of version 1 of the same proxy.
   *
   * <p>
   * Local variable {@code base16EncodedBytes} was generated from the following instance of version 1 of the
   * {@code FakeNonSerializableClassProxy} class:
   * </p>
   *
   * <pre>
   * new FakeNonSerializableClassProxy(42, 'x', "2112")
   * </pre>
   *
   * <p>
   * Version 1 of the {@code FakeNonSerializableClassProxy} class is provided below:
   * </p>
   *
   * <pre>
   * private static final class FakeNonSerializableClassProxy implements Externalizable {
   *   private static final long serialVersionUID = 8158520333069502854L;
   *
   *   private static final long CURRENT_VERSION = 1L;
   *
   *   private final VersionedProxySupport versionedProxySupport = new VersionedProxySupport(this);
   *
   *   private int field1;
   *   private char field2;
   *   private String field3;
   *
   *   FakeNonSerializableClassProxy(final int field1, final char field2, final String field3) {
   *     this.field1 = field1;
   *     this.field2 = field2;
   *     this.field3 = field3;
   *   }
   *
   *   &#64;Override
   *   public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
   *     versionedProxySupport.read(in);
   *   }
   *
   *   &#64;SuppressWarnings("unused")
   *   private void readExternalV1(final ObjectInput in) throws IOException, ClassNotFoundException {
   *     field1 = in.readInt();
   *     field2 = in.readChar();
   *     field3 = (String) in.readObject();
   *   }
   *
   *   &#64;Override
   *   public void writeExternal(final ObjectOutput out) throws IOException {
   *     versionedProxySupport.write(out, CURRENT_VERSION);
   *   }
   *
   *   &#64;SuppressWarnings("unused")
   *   private void writeExternalV1(final ObjectOutput out) throws IOException {
   *     out.writeInt(field1);
   *     out.writeChar(field2);
   *     out.writeObject(field3);
   *   }
   * }
   * </pre>
   */
  @Test
  public void proxy_ShouldBeAbleToReadPreviousVersion() throws Exception {
    final String base16EncodedBytes = ""
        + "ACED00057372005867616D65732E73747261746567792E70657273697374656E" // ....sr.Xgames.strategy.persisten
        + "63652E73657269616C697A61626C652E56657273696F6E656450726F78795465" // ce.serializable.VersionedProxyTe
        + "73742446616B654E6F6E53657269616C697A61626C65436C61737350726F7879" // st$FakeNonSerializableClassProxy
        + "7138E3046968C9860C00007870770E00000000000000010000002A0078740004" // q8..ih.....xpw............*.xt..
        + "3231313278" //////////////////////////////////////////////////////// 2112x
        + "";
    final FakeNonSerializableClass expected =
        new FakeNonSerializableClass(42L, "2112", FakeNonSerializableClassProxy.FIELD4_DEFAULT_VALUE);

    final Object actual = TestProxyUtil.deserializeFromBase16EncodedBytes(base16EncodedBytes);

    assertThat(actual, is(expected));
  }

  @Test
  public void proxy_ShouldBeAbleToReadCurrentVersion() throws Exception {
    final FakeNonSerializableClass expected = new FakeNonSerializableClass(2112L, "42", 11.5);
    final byte[] serializedBytes = TestProxyUtil.serialize(new FakeNonSerializableClassProxy(expected));

    final Object actual = TestProxyUtil.deserialize(serializedBytes);

    assertThat(actual, is(expected));
  }

  /**
   * This definition represents version 2 of a class to be serialized via proxy.
   *
   * <p>
   * The differences from version 1 are as follows:
   * </p>
   *
   * <ul>
   * <li>{@code field1} now has type {@code long}; it previously had type {@code int}.</li>
   * <li>{@code field2} has been removed; it previously was of type {@code char}.</li>
   * <li>{@code field3} is unchanged.</li>
   * <li>{@code field4} is a new field; it previously did not exist.</li>
   * </ul>
   */
  private static final class FakeNonSerializableClass {
    private final long field1;
    private final String field3;
    private final double field4;

    FakeNonSerializableClass(final long field1, final String field3, final double field4) {
      this.field1 = field1;
      this.field3 = field3;
      this.field4 = field4;
    }

    long getField1() {
      return field1;
    }

    String getField3() {
      return field3;
    }

    double getField4() {
      return field4;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      } else if (!(obj instanceof FakeNonSerializableClass)) {
        return false;
      }

      final FakeNonSerializableClass other = (FakeNonSerializableClass) obj;
      return (field1 == other.field1)
          && Objects.equals(field3, other.field3)
          && (Double.compare(field4, other.field4) == 0);
    }

    @Override
    public int hashCode() {
      return Objects.hash(field1, field3, field4);
    }

    @Override
    public String toString() {
      return String.format("FakeNonSerializableClass[field1=%d, field3=%s, field4=%f]", field1, field3, field4);
    }
  }

  /**
   * This definition represents version 2 of the serializable proxy for the {@link FakeNonSerializableClass} class.
   */
  private static final class FakeNonSerializableClassProxy implements Externalizable {
    private static final long serialVersionUID = 8158520333069502854L;

    private static final long CURRENT_VERSION = 2L;

    static final double FIELD4_DEFAULT_VALUE = 3.5;

    private final VersionedProxySupport versionedProxySupport = new VersionedProxySupport(this);

    private long field1;
    private String field3;
    private double field4;

    public FakeNonSerializableClassProxy() {}

    FakeNonSerializableClassProxy(final FakeNonSerializableClass subject) {
      field1 = subject.getField1();
      field3 = subject.getField3();
      field4 = subject.getField4();
    }

    private Object readResolve() {
      return new FakeNonSerializableClass(field1, field3, field4);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
      versionedProxySupport.read(in);
    }

    @SuppressWarnings("unused")
    private void readExternalV1(final ObjectInput in) throws IOException, ClassNotFoundException {
      field1 = in.readInt(); // convert type
      in.readChar(); // skip removed field
      field3 = (String) in.readObject(); // no change in type
      field4 = FIELD4_DEFAULT_VALUE; // use default value for new field
    }

    @SuppressWarnings("unused")
    private void readExternalV2(final ObjectInput in) throws IOException, ClassNotFoundException {
      field1 = in.readLong();
      field3 = (String) in.readObject();
      field4 = in.readDouble();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
      versionedProxySupport.write(out, CURRENT_VERSION);
    }

    @SuppressWarnings("unused")
    private void writeExternalV2(final ObjectOutput out) throws IOException {
      out.writeLong(field1);
      out.writeObject(field3);
      out.writeDouble(field4);
    }
  }
}
