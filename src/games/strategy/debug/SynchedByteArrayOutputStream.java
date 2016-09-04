package games.strategy.debug;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Allows data written to a byte output stream to be read
 * safely friom a seperate thread.
 * Only readFully() is currently threadSafe for reading.
 */
class SynchedByteArrayOutputStream extends ByteArrayOutputStream {
  private final Object lock = new Object();
  private final PrintStream m_mirror;

  SynchedByteArrayOutputStream(final PrintStream mirror) {
    m_mirror = mirror;
  }

  public void write(final byte b) throws IOException {
    synchronized (lock) {
      m_mirror.write(b);
      super.write(b);
      lock.notifyAll();
    }
  }

  @Override
  public void write(final byte[] b, final int off, final int len) {
    synchronized (lock) {
      super.write(b, off, len);
      m_mirror.write(b, off, len);
      lock.notifyAll();
    }
  }

  /**
   * Read all data written to the stream.
   * Blocks until data is available.
   * This is currently the only threadsafe method for reading.
   */
  public String readFully() {
    synchronized (lock) {
      if (super.size() == 0) {
        try {
          lock.wait();
        } catch (final InterruptedException ie) {
        }
      }
      final String s = toString();
      reset();
      return s;
    }
  }
}
