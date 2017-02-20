package games.strategy.net.nio;

import java.io.ByteArrayOutputStream;

/**
 * Povide access to the raw buffer.
 */
class ByteArrayOutputStream2 extends ByteArrayOutputStream {
  public ByteArrayOutputStream2() {
    super();
  }

  public ByteArrayOutputStream2(final int size) {
    super(size);
  }

  public byte[] getBuffer() {
    return buf;
  }
}
