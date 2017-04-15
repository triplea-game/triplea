package games.strategy.persistence.serializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

/**
 * A collection of utility methods for testing serializable proxies.
 */
public final class TestProxyUtil {
  private TestProxyUtil() {}

  /**
   * Gets the Java declaration of the base16-encoded serialized representation of the specified object.
   *
   * <p>
   * This method returns a string of the form:
   * </p>
   *
   * <pre>
   * final String base16EncodedBytes = ""
   *     + "&lt;base16 chars&gt;" // &lt;ascii chars&gt;
   *       . . .
   *     + "&lt;base16 chars&gt;" // &lt;ascii chars&gt;
   *     + "";
   * </pre>
   *
   * <p>
   * It is expected that test authors will use this method to generate a snapshot of the serialized form of a
   * serializable proxy before upgrading to a later version. In that respect, the test author will be able to verify
   * that the new version of the serializable proxy can read a previous version by bulding up a test suite with a
   * test for each supported previous version.
   * </p>
   *
   * @param obj The object to serialize; may be {@code null}.
   *
   * @return The Java declaration of the base16-encoded serialized representation of the specified object; never
   *         {@code null}.
   *
   * @throws IOException If an I/O error occurs.
   */
  public static String getBase16EncodedSerializedObjectDeclaration(final Serializable obj) throws IOException {
    final StringBuilder sb = new StringBuilder();
    final String lineSeparator = System.lineSeparator();

    sb.append("final String base16EncodedBytes = \"\"");
    sb.append(lineSeparator);

    final int charsPerLine = 64;
    for (final String line : splitIntoFixedLengthChunks(encodeBase16(serialize(obj)), charsPerLine)) {
      final int slashCount = charsPerLine - line.length();
      sb.append(String.format("    + \"%s\" //%s %s", line, Strings.repeat("/", slashCount), toAsciiChars(line)));
      sb.append(lineSeparator);
    }

    sb.append("    + \"\";");
    // no final newline

    return sb.toString();
  }

  /**
   * Serializes the specified object and returns its serialized representation.
   *
   * <p>
   * This method is intended to directly serialize a serializable proxy. It is not suitable for serializing an arbitrary
   * object that may require the support of persistence delegates.
   * </p>
   *
   * @param obj The object to serialize; may be {@code null}.
   *
   * @return The serialized representation of the specified object; never {@code null}.
   *
   * @throws IOException If an I/O error occurs.
   */
  static byte[] serialize(final Serializable obj) throws IOException {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (final java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
        oos.writeObject(obj);
      }
      return baos.toByteArray();
    }
  }

  private static String encodeBase16(final byte[] bytes) {
    return BaseEncoding.base16().encode(bytes);
  }

  private static List<String> splitIntoFixedLengthChunks(final String text, final int charsPerChunk) {
    return Splitter.fixedLength(charsPerChunk).splitToList(text);
  }

  private static String toAsciiChars(final String base16EncodedBytes) {
    assert base16EncodedBytes.length() % 2 == 0;

    final StringBuilder sb = new StringBuilder();

    for (final String base16EncodedByte : splitIntoFixedLengthChunks(base16EncodedBytes, 2)) {
      final int codePoint = Integer.parseInt(base16EncodedByte, 16);
      if ((codePoint >= 0x20) && (codePoint < 0x7F)) {
        sb.appendCodePoint(codePoint);
      } else {
        sb.append('.');
      }
    }

    return sb.toString();
  }

  /**
   * Deserializes an object from the specified base16-encoded serialized representation.
   *
   * <p>
   * This method is intended to directly deserialize a serializable proxy. It is not suitable for deserializing an
   * arbitrary object that may require the support of persistence delegates.
   * </p>
   *
   * @param base16EncodedBytes The base16-encoded serialized representation of the object to deserialize; must not be
   *        {@code null}.
   *
   * @return The deserialized object; may be {@code null}.
   *
   * @throws IOException If an I/O error occurs.
   * @throws ClassNotFoundException If the class for an object being restored cannot be found.
   */
  static Object deserializeFromBase16EncodedBytes(final String base16EncodedBytes)
      throws IOException, ClassNotFoundException {
    assert base16EncodedBytes != null;

    return deserialize(decodeBase16(base16EncodedBytes));
  }

  private static byte[] decodeBase16(final String base16EncodedBytes) {
    return BaseEncoding.base16().decode(base16EncodedBytes);
  }

  /**
   * Deserializes an object from the specified serialized representation.
   *
   * <p>
   * This method is intended to directly deserialize a serializable proxy. It is not suitable for deserializing an
   * arbitrary object that may require the support of persistence delegates.
   * </p>
   *
   * @param bytes The serialized representation of the object to deserialize; must not be {@code null}.
   *
   * @return The deserialized object; may be {@code null}.
   *
   * @throws IOException If an I/O error occurs.
   * @throws ClassNotFoundException If the class for an object being restored cannot be found.
   */
  static Object deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
    assert bytes != null;

    try (final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        final java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais)) {
      return ois.readObject();
    }
  }
}
