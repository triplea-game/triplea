package games.strategy.net.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A packet of data to be written over the network.
 *
 * <p>Packets do not correspond to ip packets. A packet is just the data for one serialized object.
 *
 * <p>The packet is written over the network as 32 bits indicating the size in bytes, then the data
 * itself.
 */
class SocketWriteData {
  private static final AtomicInteger counter = new AtomicInteger();
  private final ByteBuffer size;
  private final ByteBuffer content;
  private final int number = counter.incrementAndGet();

  SocketWriteData(final byte[] data) {
    content = ByteBuffer.allocate(data.length);
    content.put(data);
    size = ByteBuffer.allocate(4);
    if (data.length > SocketReadData.MAX_MESSAGE_SIZE) {
      throw new IllegalStateException("Invalid message size: " + data.length);
    }
    size.putInt(data.length ^ SocketReadData.MAGIC);
    size.flip();
    content.flip();
  }

  /**
   * Writes any pending data to the specified channel.
   *
   * @return true if the write has written the entire message.
   */
  boolean write(final SocketChannel channel) throws IOException {
    if (size.hasRemaining()) {
      final int count = channel.write(size);
      if (count == -1) {
        throw new IOException("triplea: end of stream detected");
      }
      // we could not write everything
      if (size.hasRemaining()) {
        return false;
      }
    }
    final int count = channel.write(content);
    if (count == -1) {
      throw new IOException("triplea: end of stream detected");
    }
    return !content.hasRemaining();
  }

  @Override
  public String toString() {
    return "<id: " + number + " size: " + content.capacity() + ">";
  }
}
